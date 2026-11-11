package com.mundodev.filafacil.cloudai.vector;

public enum DocumentFamily {
    REGRAS_DE_COLETA("regras_de_coleta"),
    POLITICA_DE_ENTREGA("politica_de_entrega"),
    TIPOS_DE_OCORRENCIA("tipos_de_ocorrencia"),
    INSTRUCOES_OPERACIONAIS("instrucoes_operacionais");

    private final String id;

    DocumentFamily(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static DocumentFamily parse(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("familia invalida");
        }
        for (DocumentFamily family : values()) {
            if (family.id.equals(raw.trim())) {
                return family;
            }
        }
        throw new IllegalArgumentException("familia invalida: " + raw);
    }
}
