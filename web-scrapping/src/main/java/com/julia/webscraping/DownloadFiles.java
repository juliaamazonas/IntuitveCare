package com.julia.webscraping;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

public class DownloadFiles {

    // Função para fazer o download de um arquivo
    public static void downloadFile(String fileURL, String destination) throws IOException {
        URL url = new URL(fileURL);
        URLConnection connection = url.openConnection();
        try (InputStream inputStream = connection.getInputStream();
             FileOutputStream outputStream = new FileOutputStream(destination)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
            System.out.println("Download concluído: " + destination);
        }
    }

    public static void main(String[] args) {
        // Links dos arquivos PDF
        String[] links = {
                "https://www.gov.br/ans/pt-br/acesso-a-informacao/participacao-da-sociedade/atualizacao-do-rol-de-procedimentos/Anexo_I_Rol_2021RN_465.2021_RN627L.2024.pdf",
                "https://www.gov.br/ans/pt-br/acesso-a-informacao/participacao-da-sociedade/atualizacao-do-rol-de-procedimentos/Anexo_II_DUT_2021_RN_465.2021_RN628.2025_RN629.2025.pdf"
        };

        // Diretório onde os PDFs serão salvos
        String downloadDir = "C:\\Users\\Julia\\Documents\\projetos-intuitvecare\\web-scrapping\\target\\downloads\\";

        File dir = new File(downloadDir);
        if (!dir.exists()) {
            dir.mkdirs();  // Cria a pasta 'downloads' caso não exista
        }


        try {

            // Baixar os arquivos PDF
            for (String link : links) {
                String fileName = link.substring(link.lastIndexOf("/") + 1);
                downloadFile(link, downloadDir + fileName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

