package com.mundodev.filafacil.inference.prompt;

public record FieldValue(String valor, String origem, String evidencia) {

    public static FieldValue fato(String valor, String evidencia) {
        return new FieldValue(valor, "fato", evidencia);
    }

    public static FieldValue ausente() {
        return new FieldValue(null, "ausente", null);
    }

    public static FieldValue desconhecido(String justificativa) {
        return new FieldValue(null, "ausente", justificativa);
    }

    public boolean preenchido() {
        return valor != null && !valor.isBlank();
    }
}
