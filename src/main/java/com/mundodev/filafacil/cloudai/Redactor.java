package com.mundodev.filafacil.cloudai;

import java.util.regex.Pattern;

public final class Redactor {

    private static final Pattern SECRET = Pattern.compile(
            "(gsk_[A-Za-z0-9]+|sk-[A-Za-z0-9]+|AQ\\.[A-Za-z0-9_\\-]+)");

    private Redactor() {
    }

    public static String mask(String text) {
        if (text == null || text.isBlank()) {
            return text == null ? "" : text;
        }
        return SECRET.matcher(text).replaceAll("***");
    }
}
