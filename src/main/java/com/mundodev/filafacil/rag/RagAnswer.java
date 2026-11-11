package com.mundodev.filafacil.rag;

import java.util.List;

public record RagAnswer(
        boolean found,
        String evidencia,
        String orientacao,
        String docId,
        String versao,
        String secao,
        String dominioUsado,
        double score) {

    public static RagAnswer missing(String dominioUsado) {
        return new RagAnswer(false, "", "", "", "", "", dominioUsado, 0);
    }

    public static RagAnswer cited(KnowledgeChunk chunk, String dominioUsado, double score) {
        return new RagAnswer(
                true,
                chunk.texto(),
                "Consulte a secao " + chunk.secao() + ".",
                chunk.docId(),
                chunk.versao(),
                chunk.secao(),
                dominioUsado,
                score);
    }

    public String render() {
        if (!found) {
            return "Nao encontrado nos documentos.";
        }
        return "evidencia: " + evidencia
                + "\norientacao: " + orientacao
                + "\ncitacao: " + docId + " | " + versao + " | " + secao;
    }

    public static String classifyStale(RagAnswer answer, String versaoVigente) {
        if (answer.found() && !versaoVigente.equals(answer.versao())) {
            return "documento_desatualizado";
        }
        return "";
    }

    public static String classifyChunk(boolean markerSplitOutOfEveryChunk) {
        return markerSplitOutOfEveryChunk ? "chunk_mal_dividido" : "";
    }

    public static String classifyFilter(boolean onlyChunkHiddenByFilter) {
        return onlyChunkHiddenByFilter ? "filtro_incorreto" : "";
    }
}
