<template>
  <div id="app">
    <header>
      <h1>Busca de Operadoras de Planos de Saúde</h1>
    </header>

    <main>
      <div class="search-container">
        <input
          type="text"
          v-model="termoBusca"
          placeholder="Digite o nome, CNPJ ou outro dado da operadora..."
          @keyup.enter="buscarOperadoras"
        />
        <button @click="buscarOperadoras">Buscar</button>
      </div>

      <div class="loading" v-if="carregando">Carregando resultados...</div>

      <div class="error" v-if="erro">
        {{ erro }}
      </div>

      <div class="results" v-if="!carregando && resultados.length > 0">
        <h2>Resultados da busca ({{ total }} encontrados)</h2>

        <table>
          <thead>
            <tr>
              <th>Registro ANS</th>
              <th>Razão Social</th>
              <th>Nome Fantasia</th>
              <th>CNPJ</th>
              <th>Modalidade</th>
              <th>UF</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(operadora, index) in resultados" :key="index">
              <td>{{ operadora.registro_ans }}</td>
              <td>{{ operadora.razao_social }}</td>
              <td>{{ operadora.nome_fantasia }}</td>
              <td>{{ operadora.cnpj }}</td>
              <td>{{ operadora.modalidade }}</td>
              <td>{{ operadora.uf }}</td>
              <td>
                <button @click="verDetalhes(operadora)">Detalhes</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div
        class="no-results"
        v-if="!carregando && buscarealizada && resultados.length === 0"
      >
        Nenhuma operadora encontrada com o termo "{{ termoBusca }}".
      </div>

      <!-- Modal de detalhes -->
      <div class="modal" v-if="showModal">
        <div class="modal-content">
          <span class="close" @click="showModal = false">&times;</span>
          <h2>Detalhes da Operadora</h2>
          <div class="details" v-if="operadoraSelecionada">
            <table>
              <tr v-for="(valor, chave) in operadoraSelecionada" :key="chave">
                <th>{{ formatarChave(chave) }}</th>
                <td>{{ valor }}</td>
              </tr>
            </table>
          </div>
        </div>
      </div>
    </main>

    <footer>
      <p>Dados provenientes da ANS - Agência Nacional de Saúde Suplementar</p>
    </footer>
  </div>
</template>

<script>
import axios from "axios";

export default {
  name: "App",
  data() {
    return {
      termoBusca: "",
      resultados: [],
      carregando: false,
      erro: null,
      total: 0,
      buscarealizada: false,
      showModal: false,
      operadoraSelecionada: null,
    };
  },
  methods: {
    async buscarOperadoras() {
      if (!this.termoBusca.trim()) {
        this.erro = "Por favor, digite um termo para busca.";
        return;
      }

      this.carregando = true;
      this.erro = null;
      this.buscarealizada = true;

      try {
        const response = await axios.get(
          `http://localhost:5000/api/operadoras/buscar`,
          {
            params: {
              termo: this.termoBusca,
            },
          }
        );

        this.resultados = response.data.resultados;
        this.total = response.data.total;
      } catch (error) {
        console.error("Erro ao buscar dados:", error);
        this.erro =
          "Ocorreu um erro ao buscar as operadoras. Verifique se o servidor está rodando.";
        this.resultados = [];
      } finally {
        this.carregando = false;
      }
    },
    verDetalhes(operadora) {
      this.operadoraSelecionada = operadora;
      this.showModal = true;
    },
    formatarChave(chave) {
      // Formata a chave do objeto para exibição amigável
      return chave.replace(/_/g, " ").replace(/\b\w/g, (l) => l.toUpperCase());
    },
  },
};
</script>

<style>
#app {
  font-family: "Avenir", Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
  margin: 0;
  padding: 0;
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

header {
  background-color: #0066cc;
  color: white;
  padding: 20px;
  text-align: center;
  border-radius: 5px;
  margin-bottom: 20px;
}

.search-container {
  display: flex;
  margin-bottom: 20px;
}

input {
  flex: 1;
  padding: 10px;
  font-size: 16px;
  border: 1px solid #ddd;
  border-radius: 5px 0 0 5px;
}

button {
  padding: 10px 20px;
  background-color: #0066cc;
  color: white;
  border: none;
  border-radius: 0 5px 5px 0;
  cursor: pointer;
  font-size: 16px;
}

button:hover {
  background-color: #0055aa;
}

table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 20px;
}

th,
td {
  border: 1px solid #ddd;
  padding: 12px;
  text-align: left;
}

th {
  background-color: #f2f2f2;
}

.loading,
.error,
.no-results {
  text-align: center;
  padding: 20px;
  font-size: 18px;
}

.error {
  color: red;
}

/* Modal styles */
.modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 100;
}

.modal-content {
  background-color: white;
  padding: 30px;
  border-radius: 5px;
  width: 80%;
  max-width: 800px;
  max-height: 80vh;
  overflow-y: auto;
}

.close {
  color: #aaa;
  float: right;
  font-size: 28px;
  font-weight: bold;
  cursor: pointer;
}

.close:hover {
  color: black;
}

footer {
  margin-top: 50px;
  text-align: center;
  color: #666;
  font-size: 14px;
}

/* Responsive */
@media (max-width: 768px) {
  table {
    display: block;
    overflow-x: auto;
  }

  .search-container {
    flex-direction: column;
  }

  input,
  button {
    width: 100%;
    border-radius: 5px;
    margin-bottom: 10px;
  }
}
</style>
