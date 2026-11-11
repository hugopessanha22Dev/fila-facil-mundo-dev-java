package com.mundodev.filafacil.aidevops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Set;

public final class OutputSchema {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Set<String> TIPOS = Set.of(
            "consulta_entrega", "ocorrencia_operacional", "reclamacao_cliente");
    private static final Set<String> PRIORIDADES = Set.of("baixa", "media", "alta");

    private OutputSchema() {
    }

    public record Parsed(
            boolean jsonValid,
            boolean schemaOk,
            String tipo,
            String resumo,
            String proximaAcao,
            String prioridade) {

        public static Parsed invalid() {
            return new Parsed(false, false, "", "", "", "");
        }
    }

    public static Parsed parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Parsed.invalid();
        }
        JsonNode node;
        try {
            node = MAPPER.readTree(raw);
        } catch (Exception ex) {
            return Parsed.invalid();
        }
        if (node == null || !node.isObject()) {
            return new Parsed(true, false, "", "", "", "");
        }
        String tipo = text(node, "tipoSolicitacao");
        String resumo = first(node, "resumo", "resumoOperacional");
        String acao = text(node, "proximaAcao");
        String prioridade = first(node, "prioridade", "prioridadeSugerida");
        boolean schema = !tipo.isBlank()
                && !resumo.isBlank()
                && !acao.isBlank()
                && !prioridade.isBlank()
                && TIPOS.contains(tipo)
                && PRIORIDADES.contains(prioridade);
        return new Parsed(true, schema, tipo, resumo, acao, prioridade);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return "";
        }
        return value.asText("").trim();
    }

    private static String first(JsonNode node, String primary, String alias) {
        String value = text(node, primary);
        if (!value.isBlank()) {
            return value;
        }
        return text(node, alias);
    }
}
