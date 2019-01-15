package com.google.blockly.utils;

public class SelectedLanuage {
    private SelectedLanuage() {
    }

    private static String lang = "en";

    public static String getLang() {
        return lang;
    }

    public static void setLang(String newLang) {
        lang = newLang;
    }
}
