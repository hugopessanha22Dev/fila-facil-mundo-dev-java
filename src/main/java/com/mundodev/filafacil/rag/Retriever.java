package com.mundodev.filafacil.rag;

import com.mundodev.filafacil.cloudai.vector.TokenEmbedder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class Retriever {

    public record Hit(KnowledgeChunk chunk, double score) {
    }

    public record SearchFilter(String role, String domain, String tipo, String versao) {

        public static SearchFilter of(String role, String domain) {
            return new SearchFilter(role, domain, null, null);
        }
    }

    public List<Hit> search(List<KnowledgeChunk> chunks, String query, SearchFilter filter, RetrievalConfig config) {
        double[] queryVector = TokenEmbedder.embed(query);
        List<Hit> hits = new ArrayList<>();
        for (KnowledgeChunk chunk : chunks) {
            if (!visible(chunk, filter)) {
                continue;
            }
            double cosine = TokenEmbedder.cosine(queryVector, chunk.vector());
            double keyword = keywordFraction(query, chunk.texto());
            double weight = config.hybridWeight();
            double score = cosine * (1 - weight) + keyword * weight;
            if (config.rerank()) {
                score += sectionOverlap(query, chunk.secao());
            }
            if (score < config.cutoff()) {
                continue;
            }
            hits.add(new Hit(chunk, score));
        }
        hits.sort(Comparator.comparingDouble(Hit::score).reversed());
        int limit = Math.max(1, config.topK());
        if (hits.size() > limit) {
            return List.copyOf(hits.subList(0, limit));
        }
        return List.copyOf(hits);
    }

    private static boolean visible(KnowledgeChunk chunk, SearchFilter filter) {
        if (filter.domain() != null && !filter.domain().equals(chunk.dominio())) {
            return false;
        }
        if (filter.tipo() != null && !filter.tipo().equals(chunk.tipo())) {
            return false;
        }
        if (filter.versao() != null && !filter.versao().equals(chunk.versao())) {
            return false;
        }
        String access = chunk.acesso();
        if ("publico".equals(access)) {
            return true;
        }
        return access.equals(filter.role());
    }

    static double keywordFraction(String query, String text) {
        List<String> tokens = tokens(query);
        if (tokens.isEmpty()) {
            return 0;
        }
        String haystack = " " + String.join(" ", tokens(text)) + " ";
        int found = 0;
        for (String token : tokens) {
            if (haystack.contains(" " + token + " ")) {
                found++;
            }
        }
        return found / (double) tokens.size();
    }

    static double sectionOverlap(String query, String section) {
        List<String> sectionTokens = tokens(section);
        if (sectionTokens.isEmpty()) {
            return 0;
        }
        String haystack = " " + String.join(" ", tokens(query)) + " ";
        int found = 0;
        for (String token : sectionTokens) {
            if (haystack.contains(" " + token + " ")) {
                found++;
            }
        }
        return found / (double) sectionTokens.size();
    }

    private static List<String> tokens(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        for (String token : text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (!token.isBlank()) {
                tokens.add(token);
            }
        }
        return tokens;
    }
}
