package com.mundodev.filafacil.excecoes.domain;

public enum Severidade {
    baixa,
    media,
    alta,
    critica;

    public boolean exigeEscala() {
        return this == alta || this == critica;
    }
}
