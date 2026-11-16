package com.mundodev.filafacil.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ContextoCompressor {

    private ContextoCompressor() {
    }

    public static List<Retriever.Hit> comprimir(List<Retriever.Hit> hits, String query) {
        List<Retriever.Hit> kept = new ArrayList<>();
        for (Retriever.Hit hit : hits) {
            if (!redundante(hit, kept, query)) {
                kept.add(hit);
            }
        }
        return List.copyOf(kept);
    }

    private static boolean redundante(Retriever.Hit hit, List<Retriever.Hit> kept, String query) {
        for (Retriever.Hit anterior : kept) {
            if (!anterior.chunk().texto().contains(hit.chunk().texto())) {
                continue;
            }
            return !termoExclusivo(query, hit.chunk().texto(), kept);
        }
        return false;
    }

    private static boolean termoExclusivo(String query, String texto, List<Retriever.Hit> kept) {
        for (String token : tokens(query)) {
            if (!texto.toLowerCase(Locale.ROOT).contains(token)) {
                continue;
            }
            boolean ausente = kept.stream().noneMatch(hit -> hit.chunk().texto().toLowerCase(Locale.ROOT).contains(token));
            if (ausente) {
                return true;
            }
        }
        return false;
    }

    private static List<String> tokens(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        for (String token : text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (token.length() > 2) {
                tokens.add(token);
            }
        }
        return tokens;
    }
}
