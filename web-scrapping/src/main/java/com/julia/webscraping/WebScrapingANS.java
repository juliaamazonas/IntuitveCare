package com.julia.webscraping;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

public class WebScrapingANS {

    public static void main(String[] args) {
        // URL da página da ANS com os Anexos I e II
        String url = "https://www.gov.br/ans/pt-br/acesso-a-informacao/participacao-da-sociedade/atualizacao-do-rol-de-procedimentos";

        try {
            // Conectar ao site e obter o HTML da página
            Document document = Jsoup.connect(url).get();

            // Imprimir o HTML completo para verificar o conteúdo
            //System.out.println(document.html());

            // Procurar os links dos anexos com a classe "internal-link" e que contenham "Anexo"
            Elements links = document.select("a.internal-link[href]");

            // Exibir os links encontrados
            for (Element link : links) {
                String linkHref = link.attr("href");
                if (linkHref.contains("Anexo_I") || linkHref.contains("Anexo_II")) {
                    System.out.println("Link encontrado: " + linkHref);
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao acessar o site: " + e.getMessage());
        }
    }
}
