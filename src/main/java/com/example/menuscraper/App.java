package com.example.menuscraper;


import com.example.menuscraper.model.MenuDay;
import org.jsoup.nodes.Document;


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class App {
public static void main(String[] args) throws Exception {
Properties cfg = new Properties();
try (InputStream in = App.class.getClassLoader().getResourceAsStream("config.properties")) {
if (in != null) cfg.load(new InputStreamReader(in, StandardCharsets.UTF_8));
}
String url = cfg.getProperty("menu.url");
if (url == null || url.isBlank()) {
System.err.println("menu.url is empty");
System.exit(1);
}


File docs = new File("docs");
File raw = new File(docs, "data/raw.html");


// 1) 페이지 수집
Document doc = Scraper.fetch(url, raw);


//2) 파싱
List<String> splitters = csv(cfg.getProperty("item.splitters", "\n,/"));
List<String> lunchKeys = csv(cfg.getProperty("keyword.lunch", "중식,점심,Lunch"));
List<String> dinnerKeys = csv(cfg.getProperty("keyword.dinner", "석식,저녁,Dinner"));
Parser parser = new Parser(splitters, lunchKeys, dinnerKeys);
List<MenuDay> days = parser.parse(doc, cfg);


//➜ [수정] 주말 제외 필터 (정확히 토/일만 제외)
List<MenuDay> weekdaysOnly = new ArrayList<>();
for (com.example.menuscraper.model.MenuDay d : days) {
 boolean isWeekend = false;

 if (d.getDate() != null) {
     switch (d.getDate().getDayOfWeek()) {
         case SATURDAY:
         case SUNDAY:
             isWeekend = true;
             break;
     }
 } else {
     String label = d.getLabel() == null ? "" : d.getLabel();
     // '토요일' 또는 '(토)'만 제외, '일요일' 또는 '(일)'만 제외
     // 영어도 Sat/Sun만 정확히 매칭
     if (label.matches(".*(토요일|\\(토\\)|\\bSat\\b).*")) isWeekend = true;
     if (label.matches(".*(일요일|\\(일\\)|\\bSun\\b).*")) isWeekend = true;
 }

 if (!isWeekend) weekdaysOnly.add(d);
}

//➜ [안전장치] 필터 후 비어버리면 원본 사용
List<MenuDay> result = weekdaysOnly.isEmpty() ? days : weekdaysOnly;

//3) 렌더링(고기 강조)
List<String> meats = csv(cfg.getProperty("meat.words", "고기,소고기,돼지고기"));
MeatHighlighter highlighter = new MeatHighlighter(meats);
Renderer renderer = new Renderer(highlighter);
renderer.render(result, docs);



System.out.println("✔ 완료: docs/index.html 생성");
}


private static List<String> csv(String s) {
List<String> out = new ArrayList<>();
for (String x : s.split(",")) out.add(x.trim());
return out;
}
}