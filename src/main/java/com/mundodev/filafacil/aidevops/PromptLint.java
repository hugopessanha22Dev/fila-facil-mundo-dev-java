package com.mundodev.filafacil.aidevops;

import com.mundodev.filafacil.cloudai.Redactor;

import java.util.List;

public record PromptLint(boolean ok, String reason) {

    private static final List<String> FIELDS = List.of(
            "tipoSolicitacao", "resumo", "proximaAcao", "prioridade");

    public static PromptLint check(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return new PromptLint(false, "prompt vazio");
        }
        if (!Redactor.mask(prompt).equals(prompt)) {
            return new PromptLint(false, "segredo no prompt");
        }
        for (String field : FIELDS) {
            if (!prompt.contains(field)) {
                return new PromptLint(false, "schema ausente: " + field);
            }
        }
        return new PromptLint(true, "");
    }
}
