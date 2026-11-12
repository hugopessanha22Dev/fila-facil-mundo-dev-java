package com.mundodev.filafacil.excecoes.domain;

public class TransicaoRecusada extends RuntimeException {

    public TransicaoRecusada(String message) {
        super(message);
    }
}
