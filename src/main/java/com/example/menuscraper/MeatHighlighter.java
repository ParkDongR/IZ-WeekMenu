package com.example.menuscraper;


import java.util.List;
import java.util.regex.Pattern;


public class MeatHighlighter {
private final List<String> meatWords;


public MeatHighlighter(List<String> meatWords) {
this.meatWords = meatWords;
}


public String highlight(String text) {
if (text == null) return "";
String result = text;
for (String w : meatWords) {
String word = w.trim();
if (word.isEmpty()) continue;
// 한글은 단어 경계가 애매하므로 단순 포함을 사용하되, 중복 래핑 방지를 위해 negative lookbehind/ahead
String regex = "(?i)(?<!</span>)(" + Pattern.quote(word) + ")(?!</span>)";
result = result.replaceAll(regex, "<span class=\"meat\">$1</span>");
}
return result;
}
}