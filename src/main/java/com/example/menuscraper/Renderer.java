package com.example.menuscraper;


import com.example.menuscraper.model.MenuDay;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class Renderer {
private final MeatHighlighter highlighter;


public Renderer(MeatHighlighter highlighter) {
this.highlighter = highlighter;
}


public void render(List<MenuDay> days, File docsDir) throws IOException {
File assets = new File(docsDir, "assets");
File data = new File(docsDir, "data");
assets.mkdirs();
data.mkdirs();

ZoneId KST = ZoneId.of("Asia/Seoul");
LocalDate today = LocalDate.now(KST);

// 라벨에서 날짜를 유추 (연도 없는 MM/dd 표기 등 지원)
DateTimeFormatter[] FMT = new DateTimeFormatter[] {
    DateTimeFormatter.ofPattern("yyyy.MM.dd"),
    DateTimeFormatter.ofPattern("yyyy-MM-dd"),
    DateTimeFormatter.ofPattern("yyyy/MM/dd"),
    DateTimeFormatter.ofPattern("M/d"),
    DateTimeFormatter.ofPattern("MM/dd"),
    DateTimeFormatter.ofPattern("M.d"),
    DateTimeFormatter.ofPattern("MM.dd"),
    DateTimeFormatter.ofPattern("M월 d일")
};

java.util.function.Function<String, LocalDate> parseLabelToDate = (label) -> {
    if (label == null) return null;
    String t = label.replaceAll("\\s+", "")
                    .replaceAll("\\(.*?\\)", "")   // (월) 같은 요일 괄호 제거
                    .replace("월요일","").replace("화요일","").replace("수요일","")
                    .replace("목요일","").replace("금요일","")
                    .replace("토요일","").replace("일요일","");
    for (DateTimeFormatter fmt : FMT) {
        try {
            // 연도 없는 포맷은 올해로 보정
            if (fmt.toString().contains("y")) {
                return LocalDate.parse(t, fmt);
            } else {
                LocalDate d = LocalDate.parse(t, fmt);
                return d.withYear(today.getYear());
            }
        } catch (DateTimeParseException ignore) {}
    }
    // 08/21 처럼 숫자만 있을 때 최후의 보정
    if (t.matches("\\d{1,2}/\\d{1,2}")) {
        String[] p = t.split("/");
        return LocalDate.of(today.getYear(), Integer.parseInt(p[0]), Integer.parseInt(p[1]));
    }
    return null;
};


// JSON 저장
File json = new File(data, "menu.json");
try (Writer w = new OutputStreamWriter(new FileOutputStream(json), StandardCharsets.UTF_8)) {
w.write(toJson(days));
}

//CSS
File css = new File(assets, "style.css");
if (!css.exists()) { // ← 이미 있으면 덮어쓰지 않음
 try (Writer w = new OutputStreamWriter(new FileOutputStream(css), StandardCharsets.UTF_8)) {
     w.write("body{font-family:system-ui,-apple-system,Segoe UI,Roboto,Pretendard,Apple SD Gothic Neo,Malgun Gothic,sans-serif;margin:24px;}\n"
             + "h1{font-size:28px;margin-bottom:8px;}\n"
             + ".day{border:1px solid #e5e7eb;border-radius:16px;padding:16px;margin:12px 0;box-shadow:0 2px 8px rgba(0,0,0,.04);}\n"
             + ".label{font-weight:700;margin-bottom:8px;font-size:18px;}\n"
             + ".meal{display:grid;grid-template-columns:120px 1fr;gap:12px;margin:6px 0;align-items:start;}\n"
             + ".meal b{display:inline-block;background:#f3f4f6;padding:6px 10px;border-radius:999px;font-weight:600;}\n"
             + ".items{line-height:1.8} .items li{margin-left:18px;}\n"
             + ".meat{font-weight:800;text-decoration:underline;}\n"
             + ".footer{margin-top:24px;color:#6b7280;font-size:12px;}\n");
 }
}


//HTML (헤더 + 간결한 표)
File html = new File(docsDir, "index.html");
try (Writer w = new OutputStreamWriter(new FileOutputStream(html), StandardCharsets.UTF_8)) {
w.write("<!doctype html><html lang=\"ko\"><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">");
w.write("<title>IZEN 한 주 식단표</title><link rel=\"stylesheet\" href=\"assets/style.css\">");
w.write("<body>");

// 상단 헤더
w.write(
 "<header class=\"site-header\">" +
   "<div class=\"brand\">" +
     "<img src=\"assets/izen-logo.png\" alt=\"IZEN\" class=\"logo\"/>" +
     "<span class=\"brand-name\"></span>" +
   "</div>" +
   "<div class=\"headline\">주간 식단표</div>" +
 "</header>"
);

// 본문 컨테이너 시작
w.write("<main class=\"container\">");
w.write("<h1 class=\"sr-only\">주간 식단(점심·저녁)</h1>");

// 간결한 3열 표: 날짜/요일 | 점심 | 저녁
w.write("<div class=\"table-wrap\"><table class=\"weekly-table\">");
w.write("<thead><tr><th>날짜/요일</th><th>점심</th><th>저녁</th></tr></thead><tbody>");

for (com.example.menuscraper.model.MenuDay d : days) {
 String label = d.getLabel() == null ? "" : d.getLabel();
 
 
 boolean isToday = false;
 if (d.getDate() != null) {
     isToday = d.getDate().equals(today);
 } else {
     LocalDate fromLabel = parseLabelToDate.apply(label);
     if (fromLabel != null) isToday = fromLabel.equals(today);
 }

//isToday 계산은 그대로 두고...
w.write("<tr" + (isToday ? " class=\"is-today\"" : "") + ">");

//⬇️ 여기 한 줄만 교체
w.write("<td class=\"day-col\">" 
      + escape(label) 
      + (isToday ? " <span class=\"badge-today\">오늘</span>" : "") 
      + "</td>");

 // 점심
 w.write("<td>");
 if (!d.getLunch().isEmpty()) {
   w.write("<ul class=\"items\">");
   for (String it : d.getLunch()) {
     w.write("<li>" + highlighter.highlight(escape(it)) + "</li>");
   }
   w.write("</ul>");
 } else {
   w.write("<span class=\"muted\">—</span>");
 }
 w.write("</td>");

 // 저녁
 w.write("<td>");
 if (!d.getDinner().isEmpty()) {
   w.write("<ul class=\"items\">");
   for (String it : d.getDinner()) {
     w.write("<li>" + highlighter.highlight(escape(it)) + "</li>");
   }
   w.write("</ul>");
 } else {
   w.write("<span class=\"muted\">—</span>");
 }
 w.write("</td>");

 w.write("</tr>");
}

w.write("</tbody></table>");
w.write("<div class=\"footer\">매일 아침 8시 자동 업뎃~>.-</div>");
w.write("</body></html>");
}

}


private String escape(String s) {
if (s == null) return "";
return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
}


private String toJson(List<MenuDay> days) {
StringBuilder sb = new StringBuilder();
sb.append("{\"days\":[");
for (int i = 0; i < days.size(); i++) {
MenuDay d = days.get(i);
if (i > 0) sb.append(',');
sb.append("{\"label\":\"").append(escape(d.getLabel())).append("\",");
sb.append("\"lunch\":[");
for (int j = 0; j < d.getLunch().size(); j++) {
if (j > 0) sb.append(',');
sb.append("\"").append(escape(d.getLunch().get(j))).append("\"");
}
sb.append("],\"dinner\":[");
for (int j = 0; j < d.getDinner().size(); j++) {
if (j > 0) sb.append(',');
sb.append("\"").append(escape(d.getDinner().get(j))).append("\"");
}
sb.append("]}");
}
sb.append("]}");
return sb.toString();
}
}

