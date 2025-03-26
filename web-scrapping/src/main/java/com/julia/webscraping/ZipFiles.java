package com.julia.webscraping;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipFiles {

    // Função para compactar os arquivos PDF
    public static void zipFiles(String[] filePaths, String zipFilePath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(zipFilePath);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            for (String filePath : filePaths) {
                File file = new File(filePath);
                try (FileInputStream fis = new FileInputStream(file)) {
                    ZipEntry zipEntry = new ZipEntry(file.getName());
                    zos.putNextEntry(zipEntry);

                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = fis.read(buffer)) != -1) {
                        zos.write(buffer, 0, length);
                    }
                    zos.closeEntry();
                }
            }
            System.out.println("Arquivos compactados com sucesso em: " + zipFilePath);
        }
    }

    public static void main(String[] args) {
        // Diretório onde os PDFs estão localizados
        String downloadDir = "C:\\Users\\Julia\\Documents\\projetos-intuitvecare\\web-scrapping\\target\\downloads\\";

        // Caminhos dos arquivos PDF para compactar
        String[] filesToZip = {
                downloadDir + "Anexo_I_Rol_2021RN_465.2021_RN627L.2024.pdf",
                downloadDir + "Anexo_II_DUT_2021_RN_465.2021_RN628.2025_RN629.2025.pdf"
        };

        // Caminho do arquivo ZIP de destino
        String zipFilePath = downloadDir + "anexos.zip";

        try {
            // Compactar os arquivos
            zipFiles(filesToZip, zipFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

