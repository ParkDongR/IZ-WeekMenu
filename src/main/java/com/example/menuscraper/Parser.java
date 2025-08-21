package com.example.menuscraper;


import com.example.menuscraper.model.MenuDay;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {
private final List<String> itemSplitters;
private final List<String> lunchKeywords;
private final List<String> dinnerKeywords;


public Parser(List<String> itemSplitters, List<String> lunchKeywords, List<String> dinnerKeywords) {
this.itemSplitters = itemSplitters;
this.lunchKeywords = lunchKeywords;
this.dinnerKeywords = dinnerKeywords;
}


public List<MenuDay> parse(Document doc, Properties cfg) {
// 1) 명시적 컨테이너/셀렉터가 있으면 우선 사용
String dayContainers = cfg.getProperty("selector.day.containers", "").trim();
if (!dayContainers.isEmpty()) {
List<MenuDay> res = parseByContainers(doc, cfg);
if (!res.isEmpty()) return res;
}
// 2) 테이블 기반 시도
List<MenuDay> table = parseByTable(doc);
if (!table.isEmpty()) return table;
// 3) 텍스트 휴리스틱
return parseByTextHeuristic(doc);
}

private List<MenuDay> parseByContainers(Document doc, Properties cfg) {
List<MenuDay> out = new ArrayList<>();
String dayContainers = cfg.getProperty("selector.day.containers");
String dayLabelSel = cfg.getProperty("selector.day.label", "");
String lunchSel = cfg.getProperty("selector.lunch", "");
String dinnerSel = cfg.getProperty("selector.dinner", "");


Elements containers = doc.select(dayContainers);
for (Element c : containers) {
MenuDay md = new MenuDay();
String label = dayLabelSel.isEmpty() ? guessDayLabel(c) : c.select(dayLabelSel).text();
md.setLabel(label);
md.getLunch().addAll(splitItems(lunchSel.isEmpty() ? guessMealText(c, lunchKeywords) : c.select(lunchSel).text()));
md.getDinner().addAll(splitItems(dinnerSel.isEmpty() ? guessMealText(c, dinnerKeywords) : c.select(dinnerSel).text()));
if (!md.getLunch().isEmpty() || !md.getDinner().isEmpty()) out.add(md);
}
return out;
}

private String guessDayLabel(Element scope) {
	// 요일명 또는 날짜 패턴 추출
	String t = scope.text();
	Matcher m = Pattern.compile("(\\d{4}[./-]\\d{1,2}[./-]\\d{1,2}|[월화수목금토일](?:요일)?)").matcher(t);
	if (m.find()) return m.group(1);
	return "";
	}


	private String guessMealText(Element scope, List<String> keys) {
	// 키워드가 들어간 가장 가까운 요소 텍스트 추정
	Elements all = scope.getAllElements();
	for (Element e : all) {
	String tx = e.ownText();
	for (String k : keys) {
	if (tx.contains(k)) {
	// label 요소면 다음 형제/부모의 텍스트를 반환
	String sib = e.nextElementSibling() != null ? e.nextElementSibling().text() : "";
	if (!sib.isEmpty()) return sib;
	return collapseSyllableBreaks(e.text());
	}
	}
	}
	return "";
	}


	private List<MenuDay> parseByTable(Document doc) {
	List<MenuDay> out = new ArrayList<>();
	for (Element table : doc.select("table")) {
	Elements ths = table.select("thead th, tr:first-child th, tr:first-child td");
	if (ths.isEmpty()) continue;
	int lunchIdx = -1, dinnerIdx = -1, dayIdx = 0;
	for (int i = 0; i < ths.size(); i++) {
	String h = normalize(ths.get(i).text());
	if (matchesAny(h, lunchKeywords)) lunchIdx = i;
	if (matchesAny(h, dinnerKeywords)) dinnerIdx = i;
	}
	if (lunchIdx == -1 && dinnerIdx == -1) continue;


	Elements rows = table.select("tr");
	for (int r = 1; r < rows.size(); r++) {
	Elements tds = rows.get(r).select("td");
	if (tds.isEmpty()) continue;
	MenuDay md = new MenuDay();
	String label = tds.size() > dayIdx ? tds.get(dayIdx).text() : "";
	md.setLabel(label);
	if (lunchIdx >= 0 && lunchIdx < tds.size()) md.getLunch().addAll(splitItems(tds.get(lunchIdx).text()));
	if (dinnerIdx >= 0 && dinnerIdx < tds.size()) md.getDinner().addAll(splitItems(tds.get(dinnerIdx).text()));
	if (!md.getLunch().isEmpty() || !md.getDinner().isEmpty()) out.add(md);
	}
	}
	return out;
	}
	
	private List<MenuDay> parseByTextHeuristic(Document doc) {
		List<MenuDay> out = new ArrayList<>();
		String text = doc.text();
		// 요일 기준으로 느슨히 분할
		String[] blocks = text.split("(?=[월화수목금토일](?:요일)?\\s?)");
		for (String b : blocks) {
		String blk = b.trim();
		if (blk.isEmpty()) continue;
		MenuDay md = new MenuDay();
		Matcher dl = Pattern.compile("^([월화수목금토일](?:요일)?|\\d{4}[./-]\\d{1,2}[./-]\\d{1,2})").matcher(blk);
		if (dl.find()) md.setLabel(dl.group(1));


		// 점심/저녁 패턴 추출
		String lunch = sliceAfter(blk, lunchKeywords);
		String dinner = sliceAfter(blk, dinnerKeywords);


		md.getLunch().addAll(splitItems(lunch));
		md.getDinner().addAll(splitItems(dinner));


		if (!md.getLunch().isEmpty() || !md.getDinner().isEmpty()) out.add(md);
		}
		return out;
		}


		private String sliceAfter(String text, List<String> keys) {
		int idx = -1;
		String found = null;
		for (String k : keys) {
		int p = text.indexOf(k);
		if (p >= 0 && (idx == -1 || p < idx)) { idx = p; found = k; }
		}
		if (idx == -1) return "";
		String sub = text.substring(idx + found.length());
		// 다음 식사 키워드 전까지만 자르기
		int next = Integer.MAX_VALUE;
		for (String k : union(lunchKeywords, dinnerKeywords)) {
		int p = sub.indexOf(k);
		if (p >= 0) next = Math.min(next, p);
		}
		if (next != Integer.MAX_VALUE) sub = sub.substring(0, next);
		return sub;
		}
		
		private boolean matchesAny(String text, List<String> keys) {
			String n = normalize(text);
			for (String k : keys) if (n.contains(normalize(k))) return true;
			return false;
			}


			private String normalize(String s) { return s == null ? "" : s.replaceAll("\\s+", "").toLowerCase(); }

			
			// 글자(한글/영문/숫자) 사이에 끼어있는 개행/공백/<br>을 제거해서 문장 복원
			private String collapseSyllableBreaks(String s) {
			    if (s == null) return "";
			    String tmp = s.replace("\r", "");

			    // <br>들을 개행으로 통일
			    tmp = tmp.replaceAll("(?i)<br\\s*/?>", "\n");

			    // 비가시 문자 제거 (zero-width space 등)
			    tmp = tmp.replace("\u200B", "").replace("\u200C", "").replace("\u200D", "").replace("\uFEFF", "");

			    // 1) 글자 사이의 개행/공백(여러 개 포함)을 제거 (한글/영문/숫자 모두)
			    //    예: "참\n 치\n 김\n 치" → "참치김치"
			    for (int i = 0; i < 3; i++) { // 여러 번 안정화
			        tmp = tmp.replaceAll("([가-힣A-Za-z0-9])\\s*[\\n\\t ]+\\s*([가-힣A-Za-z0-9])", "$1$2");
			    }

			    // 2) 남은 개행은 공백으로
			    tmp = tmp.replaceAll("[\\n\\t]+", " ");

			    // 3) 공백 정리
			    tmp = tmp.replaceAll("\\s{2,}", " ").trim();
			    return tmp;
			}


			

			private List<String> splitItems(String raw) {
			    List<String> out = new ArrayList<>();
			    if (raw == null) return out;

			    // (A) 먼저 글자 사이로 갈린 개행들을 합쳐서 문장 복원
			    String tmp = collapseSyllableBreaks(raw);

			    // (B) 아이템 구분자 통일: 콤마/세미콜론/중점/파이프/슬래시/점 등
			    //    한국어 콤마 '，'도 포함
			    tmp = tmp.replaceAll("\\s*[，,;·•|/]+\\s*", "\n");

			    // (C) config에서 추가로 지정한 분리자 적용 (빈/공백 분리자 무시)
			    for (String sp : itemSplitters) {
			        String s = sp == null ? "" : sp.trim();
			        if (s.isEmpty() || " ".equals(s)) continue;
			        tmp = tmp.replace(s, "\n");
			    }

			    // (D) 다중 개행 정리
			    tmp = tmp.replaceAll("\\n{2,}", "\n");

			    // (E) 라인 정리 + 라벨/노이즈 제거
			    for (String line : tmp.split("\\n")) {
			        String t = line.trim();
			        if (t.isEmpty()) continue;

			        // "점심", "저녁" 같은 라벨 제거
			        if (t.matches("^(점심|중식|Lunch|저녁|석식|Dinner)\\s*:?$")) continue;

			        // "점심:" 처럼 접두 라벨 제거
			        t = t.replaceAll("^(점심|중식|Lunch|저녁|석식|Dinner)\\s*[:：-]\\s*", "");

			        // 항목 앞 라벨(예: "메인:", "국물-") 제거
			        t = t.replaceAll("^[^:：-]+[:：-]\\s*", "");

			        if (!t.isEmpty()) out.add(t);
			    }
			    return out;
			}




			private List<String> union(List<String> a, List<String> b) {
			List<String> u = new ArrayList<>(a);
			for (String x : b) if (!u.contains(x)) u.add(x);
			return u;
			}
			}