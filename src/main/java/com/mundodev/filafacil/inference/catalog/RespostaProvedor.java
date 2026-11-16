package com.mundodev.filafacil.inference.catalog;

public record RespostaProvedor(
        String texto,
        long latenciaMs,
        double custo,
        int tokens,
        boolean falha,
        String tipoFalha) {
}
