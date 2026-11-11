package com.mundodev.filafacil.rag;

import com.mundodev.filafacil.cloudai.vector.TokenEmbedder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Chunker {

    public List<KnowledgeChunk> chunk(SourceDocument doc, int chunkSize, int overlap) {
        int size = Math.max(1, chunkSize);
        int stepOverlap = Math.max(0, Math.min(overlap, size - 1));
        Map<String, KnowledgeChunk> unique = new LinkedHashMap<>();
        for (SourceDocument.Section secao : doc.secoes()) {
            String cleaned = clean(secao.texto());
            if (cleaned.isBlank()) {
                continue;
            }
            for (String piece : windows(cleaned, size, stepOverlap)) {
                String key = doc.versao() + "|" + secao.nome() + "|" + piece.toLowerCase(Locale.ROOT);
                unique.putIfAbsent(key, toChunk(doc, secao.nome(), piece));
            }
        }
        return List.copyOf(unique.values());
    }

    private static KnowledgeChunk toChunk(SourceDocument doc, String secao, String texto) {
        return new KnowledgeChunk(
                doc.id() + ":" + doc.versao() + ":" + secao + ":" + texto.length(),
                doc.id(),
                doc.titulo(),
                doc.tipo(),
                doc.dominio(),
                doc.versao(),
                doc.dataAtualizacao(),
                doc.owner(),
                doc.acesso(),
                secao,
                texto,
                TokenEmbedder.embed(texto));
    }

    static String clean(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("\\s+", " ").trim();
    }

    private static List<String> windows(String text, int size, int overlap) {
        if (text.length() <= size) {
            return List.of(text);
        }
        List<String> pieces = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + size);
            pieces.add(text.substring(start, end));
            if (end == text.length()) {
                break;
            }
            int next = end - overlap;
            if (next <= start) {
                break;
            }
            start = next;
        }
        return pieces;
    }
}
