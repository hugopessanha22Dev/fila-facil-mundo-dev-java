package com.mundodev.filafacil.cloudai.functions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mundodev.filafacil.cloudai.Redactor;

public final class ResponseNormalizer {

    public record Reply(String tipo, String resumo, String proximaAcao) {
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ResponseNormalizer() {
    }

    public static Reply normalize(String raw) {
        String safe = Redactor.mask(raw);
        try {
            JsonNode node = MAPPER.readTree(safe);
            if (node.isObject()) {
                String tipo = text(node, "tipoSolicitacao", "consulta_entrega");
                String resumo = firstText(node, "resumo", "resumoOperacional");
                String acao = text(node, "proximaAcao", "revisar manualmente");
                if (acao.isBlank()) {
                    acao = "revisar manualmente";
                }
                return new Reply(tipo, resumo, acao);
            }
        } catch (Exception ignored) {
            // texto livre vira resumo
        }
        String resumo = safe == null ? "" : safe.trim();
        return new Reply("consulta_entrega", resumo, "revisar manualmente");
    }

    private static String firstText(JsonNode node, String primary, String fallback) {
        String value = text(node, primary, "");
        if (!value.isBlank()) {
            return value;
        }
        return text(node, fallback, "");
    }

    private static String text(JsonNode node, String field, String def) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return def;
        }
        String text = value.asText("");
        return text.isBlank() ? def : text;
    }
}
