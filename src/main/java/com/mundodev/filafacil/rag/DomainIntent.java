package com.mundodev.filafacil.rag;

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
            new String[]{"atendimento", "atendimento"});

    private DomainIntent() {
    }

    public static String domainOf(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        for (String[] rule : RULES) {
            if (lower.contains(rule[0])) {
                return rule[1];
            }
        }
        return null;
    }
}
