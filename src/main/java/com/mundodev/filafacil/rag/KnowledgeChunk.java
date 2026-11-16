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
        String status,
        String conteudoHash,
        String tenant,
        String namespace,
        double[] vector) {

    public KnowledgeChunk substituido() {
        return new KnowledgeChunk(
                id, docId, titulo, tipo, dominio, versao, dataAtualizacao, owner, acesso, secao, texto,
                "substituido", conteudoHash, tenant, "historico", vector);
    }
}
