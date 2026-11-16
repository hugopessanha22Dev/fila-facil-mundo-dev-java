package com.mundodev.filafacil.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class QueryExpander {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Map<String, String> TERMOS = load();

    private QueryExpander() {
    }

    public static String expand(String query) {
        if (query == null || query.isBlank()) {
            return query == null ? "" : query;
        }
        String lower = query.toLowerCase(Locale.ROOT);
        StringBuilder extra = new StringBuilder();
        for (String token : tokens(query)) {
            String termo = TERMOS.get(token);
            if (termo != null && !lower.contains(termo)) {
                extra.append(' ').append(termo);
            }
        }
        return (query + extra).trim();
    }

    public static String trocaDominio(String query, String outro) {
        String atual = query == null ? "" : query;
        for (String gatilho : DomainIntent.gatilhos()) {
            atual = atual.replaceAll("(?i)" + Pattern.quote(gatilho), " ");
        }
        return (atual.replaceAll("\\s+", " ").trim() + " " + outro).trim();
    }

    private static Map<String, String> load() {
        try (InputStream in = QueryExpander.class.getResourceAsStream("/rag/expansao.json")) {
            if (in == null) {
                throw new IllegalStateException("expansao.json ausente");
            }
            Map<String, String> termos = new LinkedHashMap<>();
            JsonNode node = MAPPER.readTree(in);
            node.fieldNames().forEachRemaining(nome -> termos.put(nome, node.path(nome).asText()));
            return Map.copyOf(termos);
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao ler expansao.json", ex);
        }
    }

    private static List<String> tokens(String text) {
        List<String> tokens = new ArrayList<>();
        for (String token : text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (!token.isBlank()) {
                tokens.add(token);
            }
        }
        return tokens;
    }
}
