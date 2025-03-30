-- Script para criar a tabela de operadoras
CREATE TABLE operadoras (
    registro_ans VARCHAR(20) PRIMARY KEY,
    cnpj VARCHAR(20),
    razao_social VARCHAR(255),
    nome_fantasia VARCHAR(255),
    modalidade VARCHAR(100),
    logradouro VARCHAR(255),
    numero VARCHAR(20),
    complemento VARCHAR(100),
    bairro VARCHAR(100),
    cidade VARCHAR(100),
    uf CHAR(2),
    cep VARCHAR(10),
    ddd VARCHAR(5),
    telefone VARCHAR(20),
    fax VARCHAR(20),
    email VARCHAR(100),
    representante VARCHAR(255),
    cargo_representante VARCHAR(100),
    data_registro_ans DATE,
    data_inicio_operacao DATE
);

-- Script para criar a tabela de demonstrações contábeis
CREATE TABLE demonstracoes_contabeis (
    id SERIAL PRIMARY KEY,
    registro_ans VARCHAR(20),
    data_trimestre DATE,
    trimestre INT,
    ano INT,
    codigo_conta VARCHAR(50),
    descricao_conta VARCHAR(255),
    valor_conta DECIMAL(15, 2),
    FOREIGN KEY (registro_ans) REFERENCES operadoras(registro_ans)
);

-- Criar índices para melhorar a performance
CREATE INDEX idx_demonstracoes_contabeis_registro_ans ON demonstracoes_contabeis(registro_ans);
CREATE INDEX idx_demonstracoes_contabeis_data ON demonstracoes_contabeis(data_trimestre);
CREATE INDEX idx_demonstracoes_contabeis_ano_trimestre ON demonstracoes_contabeis(ano, trimestre);
CREATE INDEX idx_demonstracoes_codigo_conta ON demonstracoes_contabeis(codigo_conta);
CREATE INDEX idx_demonstracoes_descricao_conta ON demonstracoes_contabeis(descricao_conta);

-- Script para facilitar a importação e processamento dos dados

ALTER TABLE demonstracoes_contabeis DROP CONSTRAINT demonstracoes_contabeis_registro_ans_fkey; 

-- Consultas Analíticas para o projeto ANS Data Processor

-- 1. Consulta para identificar as 10 operadoras com maiores despesas em 
-- "EVENTOS/ SINISTROS CONHECIDOS OU AVISADOS DE ASSISTÊNCIA A SAÚDE MEDICO HOSPITALAR" 
-- no último trimestre disponível

WITH ultimo_trimestre AS (
    SELECT ano, trimestre
    FROM demonstracoes_contabeis
    ORDER BY ano DESC, trimestre DESC
    LIMIT 1
)
SELECT 
    o.registro_ans,
    o.razao_social,
    o.nome_fantasia,
    o.modalidade,
    ABS(SUM(dc.valor_conta)) as valor_despesa
FROM 
    demonstracoes_contabeis dc
JOIN 
    operadoras o ON dc.registro_ans = o.registro_ans
JOIN 
    ultimo_trimestre ut ON dc.ano = ut.ano AND dc.trimestre = ut.trimestre
WHERE 
    dc.descricao_conta ILIKE '%EVENTOS/ SINISTROS CONHECIDOS OU AVISADOS  DE ASSISTÊNCIA A SAÚDE MEDICO HOSPITALAR%'
GROUP BY 
    o.registro_ans, o.razao_social, o.nome_fantasia, o.modalidade
ORDER BY 
    valor_despesa DESC
LIMIT 10;

-- 2. Consulta para identificar as 10 operadoras com maiores despesas em
-- "EVENTOS/ SINISTROS CONHECIDOS OU AVISADOS DE ASSISTÊNCIA A SAÚDE MEDICO HOSPITALAR"
-- no último ano disponível

WITH ultimo_ano AS (
    SELECT ano
    FROM demonstracoes_contabeis
    ORDER BY ano DESC
    LIMIT 1
)
SELECT 
    o.registro_ans,
    o.razao_social,
    o.nome_fantasia,
    o.modalidade,
    ABS(SUM(dc.valor_conta)) as valor_despesa_anual
FROM 
    demonstracoes_contabeis dc
JOIN 
    operadoras o ON dc.registro_ans = o.registro_ans
JOIN 
    ultimo_ano ua ON dc.ano = ua.ano
WHERE 
    dc.descricao_conta ILIKE '%EVENTOS/ SINISTROS CONHECIDOS OU AVISADOS  DE ASSISTÊNCIA A SAÚDE MEDICO HOSPITALAR%'
GROUP BY 
    o.registro_ans, o.razao_social, o.nome_fantasia, o.modalidade
ORDER BY 
    valor_despesa_anual DESC
LIMIT 10;
