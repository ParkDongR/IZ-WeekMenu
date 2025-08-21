package com.example.menuscraper.model;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


public class MenuDay {
private String label; // 예: 2025-08-21(목) 또는 월요일
private LocalDate date; // 알 수 없으면 null
private List<String> lunch = new ArrayList<>();
private List<String> dinner = new ArrayList<>();


public String getLabel() { return label; }
public void setLabel(String label) { this.label = label; }


public LocalDate getDate() { return date; }
public void setDate(LocalDate date) { this.date = date; }


public List<String> getLunch() { return lunch; }
public List<String> getDinner() { return dinner; }
}