package com.mundodev.filafacil.pipeline.model;

public record ColetaRaw(
        String codigoColeta,
        String cliente,
        String regiao,
        String responsavel,
        String status,
        String prazoPrevisto,
        String prazoRealizado,
        String dataOperacao) {
}
