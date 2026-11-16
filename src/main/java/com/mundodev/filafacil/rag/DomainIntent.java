package com.mundodev.filafacil.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DomainIntent {

    private static final List<String[]> RULES = List.of(
            new String[]{"comprovante", "comprovantes"},
            new String[]{"ocorrencia", "ocorrencias"},
            new String[]{"parceiro", "parceiros"},
            new String[]{"atraso", "atraso"},
            new String[]{"coleta", "coletas"},
            new String[]{"entrega", "entregas"},
            new String[]{"faq", "atendimento"},
            new String[]{"atendimento", "atendimento"},
            new String[]{"decisao", "historico"},
            new String[]{"historico", "historico"});

    private DomainIntent() {
    }

    public static String domainOf(String text) {
        List<String> dominios = dominiosDe(text);
        return dominios.isEmpty() ? null : dominios.get(0);
    }

    public static List<String> dominiosDe(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String lower = text.toLowerCase(Locale.ROOT);
        List<String> found = new ArrayList<>();
        for (String[] rule : RULES) {
            if (lower.contains(rule[0]) && !found.contains(rule[1])) {
                found.add(rule[1]);
            }
        }
        return List.copyOf(found);
    }

    static List<String> gatilhos() {
        return RULES.stream().map(rule -> rule[0]).toList();
    }
}
