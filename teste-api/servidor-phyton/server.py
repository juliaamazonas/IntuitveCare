from flask import Flask, request, jsonify
from flask_cors import CORS
import pandas as pd
import re

app = Flask(__name__)
CORS(app)  # Habilita CORS para permitir requisições do frontend Vue.js

# Carregamento do arquivo CSV
def load_data():
    try:
        # Ajuste o caminho do arquivo conforme necessário
        # Este é o nome comum do arquivo, mas pode mudar dependendo do download
        file_path = 'Relatorio_cadop.csv'
        
        # Carregando o CSV com encoding correto para caracteres especiais em português
        df = pd.read_csv(file_path, sep=';', encoding='latin1')
        
        # Renomeando colunas para facilitar o acesso (removendo espaços e caracteres especiais)
        df.columns = [re.sub(r'[^a-zA-Z0-9]', '_', col.lower()) for col in df.columns]
        
        return df
    except Exception as e:
        print(f"Erro ao carregar o arquivo CSV: {e}")
        return None

# Rota para busca de operadoras
@app.route('/api/operadoras/buscar', methods=['GET'])
def buscar_operadoras():
    df = load_data()
    if df is None:
        return jsonify({"error": "Erro ao carregar os dados"}), 500
    
    # Obtendo o termo de busca da query string
    termo_busca = request.args.get('termo', '').lower()
    
    if not termo_busca:
        return jsonify({"error": "Termo de busca não fornecido"}), 400
    
    # Realizando a busca em várias colunas para maior relevância
    # Ajuste as colunas conforme o arquivo CSV real
    colunas_busca = [
        'razao_social', 'nome_fantasia', 'cnpj', 'registro_ans',
        'classificacao', 'modalidade', 'logradouro', 'municipio', 'uf'
    ]
    
    # Criando máscara de filtragem para cada coluna
    resultado = pd.DataFrame()
    for coluna in colunas_busca:
        if coluna in df.columns:
            # Convertendo valores da coluna para string antes de fazer a busca
            mascara = df[coluna].astype(str).str.lower().str.contains(termo_busca, na=False)
            if resultado.empty:
                resultado = df[mascara]
            else:
                resultado = pd.concat([resultado, df[mascara]]).drop_duplicates()
    
    # Limitando resultados para melhorar a performance (ajuste conforme necessário)
    resultado = resultado.head(50)
    
    # Convertendo para dicionário e depois para JSON
    resultado_json = resultado.to_dict(orient='records')
    
    return jsonify({
        "total": len(resultado_json),
        "resultados": resultado_json
    })

# Rota para listar todas as operadoras (paginada)
@app.route('/api/operadoras', methods=['GET'])
def listar_operadoras():
    df = load_data()
    if df is None:
        return jsonify({"error": "Erro ao carregar os dados"}), 500
    
    # Obtendo parâmetros de paginação
    pagina = int(request.args.get('pagina', 1))
    limite = int(request.args.get('limite', 20))
    
    # Calculando índices para paginação
    inicio = (pagina - 1) * limite
    fim = inicio + limite
    
    # Selecionando dados da página atual
    dados_paginados = df.iloc[inicio:fim]
    
    # Convertendo para dicionário e depois para JSON
    resultado_json = dados_paginados.to_dict(orient='records')
    
    return jsonify({
        "total": len(df),
        "pagina": pagina,
        "limite": limite,
        "resultados": resultado_json
    })

if __name__ == '__main__':
    app.run(debug=True, port=5000)