package com.challenge.api.core;

public class Album {

    private String title;
    private Integer year;
    private String format;

    private  String label;

    public Album(String title, Integer year, String format, String label) {
        this.title = title;
        this.year = year;
        this.format = format;
        this.label = label;
    }

    public String getTitle() {
        return title;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }
}
