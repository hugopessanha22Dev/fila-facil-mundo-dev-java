package com.mundodev.filafacil.pipeline.inter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

final class TimeParse {

    private static final DateTimeFormatter[] FORMATS = {
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ISO_LOCAL_DATE
    };

    private TimeParse() {
    }

    static LocalDateTime parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return LocalDateTime.MIN;
        }
        String v = raw.trim();
        for (DateTimeFormatter f : FORMATS) {
            try {
                if (f == DateTimeFormatter.ISO_LOCAL_DATE && v.length() == 10) {
                    return java.time.LocalDate.parse(v).atStartOfDay();
                }
                return LocalDateTime.parse(v, f);
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new IllegalArgumentException("timestamp invalido: " + raw);
    }

    static String dtOf(LocalDateTime ts) {
        if (ts == null || ts.equals(LocalDateTime.MIN)) {
            return "1970-01-01";
        }
        return ts.toLocalDate().toString();
    }
}
