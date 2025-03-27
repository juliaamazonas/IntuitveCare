package com.julia.extracao;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class PdfDownloader {

    public static void main(String[] args) {
        // URL do PDF
        String pdfUrl = "https://www.gov.br/ans/pt-br/acesso-a-informacao/participacao-da-sociedade/atualizacao-do-rol-de-procedimentos/Anexo_I_Rol_2021RN_465.2021_RN627L.2024.pdf";
        // Caminho para salvar o PDF localmente
        String savePath = "anexo_I.pdf";

        try {
            downloadPdf(pdfUrl, savePath);
            System.out.println("PDF baixado com sucesso: " + savePath);
        } catch (IOException e) {
            System.err.println("Erro ao baixar o PDF: " + e.getMessage());
        }
    }

    public static void downloadPdf(String fileUrl, String savePath) throws IOException {
        URL url = new URL(fileUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);

        try (InputStream inputStream = connection.getInputStream();
             FileOutputStream outputStream = new FileOutputStream(savePath)) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        connection.disconnect();
    }
}
