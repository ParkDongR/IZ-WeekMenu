package com.example.menuscraper;


import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;


public class Scraper {
public static Document fetch(String url, File debugRawHtml) throws IOException {
	Connection conn = Jsoup.connect(url)
	        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0 Safari/537.36 MenuBot/1.0")
	        .timeout(20_000)
	        .followRedirects(true);


Document doc = conn.get();


if (debugRawHtml != null) {
debugRawHtml.getParentFile().mkdirs();
try (FileWriter fw = new FileWriter(debugRawHtml, StandardCharsets.UTF_8)) {
fw.write(doc.outerHtml());
}
}
return doc;
}
}