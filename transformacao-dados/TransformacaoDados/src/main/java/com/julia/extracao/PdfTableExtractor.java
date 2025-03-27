package com.julia.extracao;

import org.apache.pdfbox.pdmodel.PDDocument;
import technology.tabula.ObjectExtractor;
import technology.tabula.PageIterator;
import technology.tabula.RectangularTextContainer;
import technology.tabula.Table;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class PdfTableExtractor {

    public static void main(String[] args) {
        // Caminho do PDF
        String pdfPath = "Anexo_I.pdf";
        String csvPath = "rol_procedimentos.csv";

        try (PDDocument document = PDDocument.load(new File(pdfPath));
             FileWriter writer = new FileWriter(csvPath)) {

            ObjectExtractor extractor = new ObjectExtractor(document);
            SpreadsheetExtractionAlgorithm sea = new SpreadsheetExtractionAlgorithm();
            PageIterator pages = extractor.extract();

            while (pages.hasNext()) {
                List<Table> tables = sea.extract(pages.next());

                for (Table table : tables) {
                    for (List<RectangularTextContainer> row : table.getRows()) {
                        StringBuilder csvRow = new StringBuilder();

                        for (RectangularTextContainer cell : row) {
                            // Método mais robusto para remover quebras de linha
                            String cellText = normalizeText(cell.getText());

                            // Adiciona o texto da célula na linha CSV, com escape de ponto e vírgula
                            csvRow.append(escapeCSV(cellText)).append(";");
                        }

                        // Remove o último ";" extra e escreve no CSV
                        writer.write(csvRow.toString().replaceAll(";$", ""));
                        writer.write("\n");
                    }
                }
            }

            System.out.println("Arquivo CSV gerado com sucesso: " + csvPath);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Método para normalizar o texto removendo quebras de linha e espaços extras
    private static String normalizeText(String text) {
        // Remove quebras de linha e substitui por espaço
        return text.replaceAll("\\r?\\n", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    // Método para escapar caracteres especiais no CSV
    private static String escapeCSV(String text) {
        // Se o texto contém ponto e vírgula, espaços ou aspas, envolve em aspas
        if (text.contains(";") || text.contains(" ") || text.contains("\"")) {
            text = text.replace("\"", "\"\"");  // Escape de aspas duplas
            text = "\"" + text + "\"";  // Envolve em aspas
        }
        return text;
    }
}