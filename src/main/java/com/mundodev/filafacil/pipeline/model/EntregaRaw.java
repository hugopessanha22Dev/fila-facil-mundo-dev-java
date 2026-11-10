package com.mundodev.filafacil.pipeline.model;

public record EntregaRaw(
        String idEntrega,
        String regiao,
        String dataConclusao,
        String prazoOriginal,
        String statusFinal,
        boolean atraso,
        String cliente,
        String unidade) {
}
