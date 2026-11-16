package com.mundodev.filafacil.inference.catalog;

public record Selecao(Modelo modelo, boolean recusada, String motivo) {

    public static Selecao aceita(Modelo modelo) {
        return new Selecao(modelo, false, "aceita");
    }

    public static Selecao recusada(String motivo) {
        return new Selecao(null, true, motivo);
    }
}
