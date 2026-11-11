package com.mundodev.filafacil.rag;

public record KnowledgeChunk(
        String id,
        String docId,
        String titulo,
        String tipo,
        String dominio,
        String versao,
        String dataAtualizacao,
        String owner,
        String acesso,
        String secao,
        String texto,
        double[] vector) {
}
