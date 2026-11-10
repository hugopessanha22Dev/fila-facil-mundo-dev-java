package com.mundodev.filafacil.pipeline.model;

public record OcorrenciaRaw(
        String tipo,
        String descricao,
        String referencia,
        String data,
        String responsavel,
        String statusResolucao) {
}
