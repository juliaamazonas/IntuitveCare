package com.ansdataprocessor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ANSDataProcessor {
    // Caminho para o diretório onde os arquivos estão salvos
    private static final String DATA_DIR = "C:\\Users\\Julia\\Documents\\projetos-intuitvecare\\banco-dados\\BancoDadosANS\\dados_ans";

    // Nome do arquivo de operadoras
    private static final String OPERADORAS_FILE = "relatorio_cadop.csv";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Configuração para PostgreSQL
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/ans_data";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "admin";

    public static void main(String[] args) {
        try {
            // Verificar se o diretório existe
            Path dataPath = Paths.get(DATA_DIR);
            if (!Files.exists(dataPath)) {
                System.err.println("O diretório " + DATA_DIR + " não existe. Verifique o caminho configurado.");
                return;
            }

            System.out.println("Usando arquivos do diretório: " + dataPath.toAbsolutePath());

            // Verificar se o arquivo de operadoras existe
            String operadorasFilePath = DATA_DIR + File.separator + OPERADORAS_FILE;
            if (!Files.exists(Paths.get(operadorasFilePath))) {
                System.err.println("Arquivo de operadoras não encontrado: " + operadorasFilePath);
                System.err.println("Certifique-se de que o arquivo 'relatorio_cadop.csv' está no diretório indicado.");
                return;
            }

            // Encontrar arquivos de demonstrações contábeis
            List<String> demonstracoesFiles = findDemonstracoesTrimestralFiles();

            if (demonstracoesFiles.isEmpty()) {
                System.err.println("Nenhum arquivo de demonstrações contábeis encontrado no diretório.");
                System.err.println("Certifique-se de que os arquivos no formato 1T2023.csv, 2T2023.csv, etc. estão no diretório.");
                return;
            }

            // Limpar tabelas e importar dados
            System.out.println("Limpando tabelas existentes...");
            limparTabelas();

            System.out.println("Importando dados de operadoras...");
            importarOperadoras(operadorasFilePath);

            for (String filePath : demonstracoesFiles) {
                File file = new File(filePath);
                String fileName = file.getName();

                // Extrair trimestre e ano do nome do arquivo
                Pattern pattern = Pattern.compile("(\\d)T(\\d{4})");
                Matcher matcher = pattern.matcher(fileName);

                if (matcher.find()) {
                    int trimestre = Integer.parseInt(matcher.group(1));
                    int ano = Integer.parseInt(matcher.group(2));
                    System.out.println("Importando demonstrações contábeis do " + trimestre + "T" + ano + "...");
                    importarDemonstracoes(filePath, trimestre, ano);
                } else {
                    System.out.println("Formato de arquivo não reconhecido: " + fileName + ". Pulando...");
                }
            }

            System.out.println("Processamento concluído com sucesso!");

        } catch (IOException | SQLException e) {
            System.err.println("Erro ao processar os dados: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void limparTabelas() throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            try (PreparedStatement pstmt = conn.prepareStatement("TRUNCATE TABLE demonstracoes_contabeis CASCADE")) {
                pstmt.executeUpdate();
            }
            try (PreparedStatement pstmt = conn.prepareStatement("TRUNCATE TABLE operadoras CASCADE")) {
                pstmt.executeUpdate();
            }
        }
    }

    private static List<String> findDemonstracoesTrimestralFiles() throws IOException {
        List<String> files = new ArrayList<>();

        // Buscar por arquivos no formato 1T2023.csv, 2T2023.csv, etc.
        Pattern filePattern = Pattern.compile("\\d{1}T\\d{4}\\.csv", Pattern.CASE_INSENSITIVE);

        Files.list(Paths.get(DATA_DIR))
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    String fileName = path.getFileName().toString();
                    if (filePattern.matcher(fileName).matches()) {
                        files.add(path.toString());
                        System.out.println("Arquivo de demonstração contábil encontrado: " + fileName);
                    }
                });

        System.out.println("Total de arquivos de demonstrações contábeis encontrados: " + files.size());
        return files;
    }

    private static void importarOperadoras(String filePath) {
        System.out.println("Importando operadoras do arquivo: " + filePath);

        String sql = "INSERT INTO operadoras (registro_ans, cnpj, razao_social, nome_fantasia, modalidade, " +
                "logradouro, numero, complemento, bairro, cidade, uf, cep, ddd, telefone, fax, " +
                "email, representante, cargo_representante, data_registro_ans, data_inicio_operacao) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql);
             BufferedReader br = new BufferedReader(new InputStreamReader(
                     Files.newInputStream(Paths.get(filePath)), StandardCharsets.UTF_8));) {

            // Ler todo o arquivo para uma lista primeiro, para debug e processamento
            List<String> fileLines = new ArrayList<>();
            String currentLine;

            // Ler o cabeçalho primeiro
            String header = br.readLine();
            if (header == null) {
                System.err.println("Arquivo de operadoras vazio!");
                return;
            }

            System.out.println("Cabeçalho do arquivo: " + header);

            // Ler a primeira linha de dados para debug
            String firstLine = br.readLine();
            if (firstLine == null) {
                System.err.println("Arquivo de operadoras não tem dados, apenas cabeçalho!");
                return;
            }

            System.out.println("Primeira linha de dados: " + firstLine);

            // Adicionar a primeira linha à lista
            fileLines.add(firstLine);

            // Ler o restante do arquivo
            while ((currentLine = br.readLine()) != null) {
                fileLines.add(currentLine);
            }

            conn.setAutoCommit(false);
            int count = 0;

            // Processar as linhas que foram lidas
            for (String dataLine : fileLines) {
                String[] data = parseCSVLine(dataLine, ';');
                if (data.length < 20) {
                    System.err.println("Linha com menos de 20 campos: " + dataLine);
                    continue; // Validação básica
                }

                for (int i = 0; i < 18; i++) {
                    pstmt.setString(i + 1, data[i]);
                }

                // Tratar datas
                String dataRegistro = data[18];
                String dataInicio = data[19];

                try {
                    if (!dataRegistro.isEmpty()) {
                        LocalDate date = LocalDate.parse(dataRegistro, DATE_FORMATTER);
                        pstmt.setDate(19, java.sql.Date.valueOf(date));
                    } else {
                        pstmt.setNull(19, java.sql.Types.DATE);
                    }

                    if (!dataInicio.isEmpty()) {
                        LocalDate date = LocalDate.parse(dataInicio, DATE_FORMATTER);
                        pstmt.setDate(20, java.sql.Date.valueOf(date));
                    } else {
                        pstmt.setNull(20, java.sql.Types.DATE);
                    }
                } catch (Exception e) {
                    // Tratar erros de formato de data
                    System.err.println("Erro ao processar datas: " + dataRegistro + " / " + dataInicio);
                    pstmt.setNull(19, java.sql.Types.DATE);
                    pstmt.setNull(20, java.sql.Types.DATE);
                }

                pstmt.addBatch();

                if (++count % 100 == 0) {
                    pstmt.executeBatch();
                    conn.commit();
                    System.out.println("Importados " + count + " registros de operadoras...");
                }
            }

            if (count % 100 != 0) {
                pstmt.executeBatch();
                conn.commit();
            }

            System.out.println("Importação de operadoras concluída. Total: " + count + " registros.");

        } catch (IOException | SQLException e) {
            System.err.println("Erro ao importar operadoras: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void importarDemonstracoes(String filePath, int trimestre, int ano) {
        System.out.println("Importando demonstrações contábeis do arquivo: " + filePath);

        String sql = "INSERT INTO demonstracoes_contabeis (registro_ans, data_trimestre, trimestre, ano, " +
                "codigo_conta, descricao_conta, valor_conta) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(
                        Files.newInputStream(Paths.get(filePath)), StandardCharsets.UTF_8))) {

                    // Ler todo o arquivo para uma lista primeiro, para debug e processamento
                    List<String> fileLines = new ArrayList<>();
                    String currentLine;

                    // Ler o cabeçalho primeiro
                    String header = br.readLine();
                    if (header == null) {
                        System.err.println("Arquivo de demonstrações vazio!");
                        return;
                    }

                    System.out.println("Cabeçalho do arquivo: " + header);

                    // Ler a primeira linha de dados para debug
                    String firstLine = br.readLine();
                    if (firstLine == null) {
                        System.err.println("Arquivo de demonstrações não tem dados, apenas cabeçalho!");
                        return;
                    }

                    System.out.println("Primeira linha de dados: " + firstLine);

                    // Adicionar a primeira linha à lista
                    fileLines.add(firstLine);

                    // Ler o restante do arquivo
                    while ((currentLine = br.readLine()) != null) {
                        fileLines.add(currentLine);
                    }

                    conn.setAutoCommit(false);
                    int count = 0;

                    // Processar as linhas que foram lidas
                    for (String dataLine : fileLines) {
                        String[] data = parseCSVLine(dataLine, ';');
                        if (data.length < 5) {
                            System.err.println("Linha com menos de 5 campos: " + dataLine);
                            continue; // Validação básica
                        }

                        // Tratar data trimestre
                        String dataTrimestre = data[0];
                        LocalDate date = null;
                        try {
                            date = LocalDate.parse(dataTrimestre, DATE_FORMATTER);
                            pstmt.setDate(2, java.sql.Date.valueOf(date));
                        } catch (Exception e) {
                            // Se a data for inválida, usar o último dia do trimestre do ano correspondente
                            int mes = trimestre * 3;
                            int dia = 31;
                            if (mes == 6) dia = 30;
                            if (mes == 9) dia = 30;
                            date = LocalDate.of(ano, mes, dia);
                            pstmt.setDate(2, java.sql.Date.valueOf(date));
                        }

                        pstmt.setString(1, data[1]);
                        pstmt.setInt(3, trimestre);
                        pstmt.setInt(4, ano);
                        pstmt.setString(5, data[2]);
                        pstmt.setString(6, data[3]);

                        // Tratar valor (converter de formato brasileiro para formato do banco)
                        String valorStr = data[4].replace(".", "").replace(",", ".");
                        try {
                            double valor = Double.parseDouble(valorStr);
                            pstmt.setDouble(7, valor);
                        } catch (NumberFormatException e) {
                            System.err.println("Erro ao converter valor: " + data[4]);
                            pstmt.setNull(7, java.sql.Types.DECIMAL);
                        }

                        pstmt.addBatch();

                        if (++count % 1000 == 0) {
                            pstmt.executeBatch();
                            conn.commit();
                            System.out.println("Importados " + count + " registros de demonstrações...");
                        }
                    }

                    if (count % 1000 != 0) {
                        pstmt.executeBatch();
                        conn.commit();
                    }

                    System.out.println("Importação de demonstrações " + trimestre + "T" + ano + " concluída. Total: " + count + " registros.");

                }
            }
        } catch (IOException | SQLException e) {
            System.err.println("Erro ao importar demonstrações: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Método melhorado para lidar com CSV que pode ter campos entre aspas
    private static String[] parseCSVLine(String line, char separator) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder field = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == separator && !inQuotes) {
                result.add(field.toString().replace("\"", "").trim());
                field = new StringBuilder();
            } else {
                field.append(c);
            }
        }

        // Adicionar o último campo
        result.add(field.toString().replace("\"", "").trim());

        return result.toArray(new String[0]);
    }
}