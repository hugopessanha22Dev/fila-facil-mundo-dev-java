package com.mundodev.filafacil.inference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mundodev.filafacil.error.InferenceException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Valida a saida do modelo contra o contrato do projeto (campos obrigatorios / enums / tipos).
 * Usa o schema em resources como referencia documental; a checagem e estrutural via Jackson.
 */
public class TriageSchemaValidator {

    private static final Set<String> TIPOS = Set.of(
            "consulta_entrega", "ocorrencia_operacional", "reclamacao_cliente");
    private static final Set<String> SENTIMENTOS = Set.of("neutro", "ansioso", "insatisfeito", "urgente");
    private static final Set<String> PRIORIDADES = Set.of("baixa", "media", "alta");
    private static final List<String> REQUIRED = List.of(
            "tipoSolicitacao", "codigoColeta", "cliente", "sentimento", "prioridadeSugerida",
            "resumoOperacional", "proximaAcao", "intervencaoHumana", "justificativaIntervencencao",
            "nivelConfianca", "camposAusentesOuErro", "indicadoresFalha");

    private final ObjectMapper mapper;

    public TriageSchemaValidator() {
        this(new ObjectMapper());
    }

    public TriageSchemaValidator(ObjectMapper mapper) {
        this.mapper = mapper;
        ensureSchemaResourcePresent();
    }

    public TriageResult validateAndParse(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            throw new InferenceException("Resposta vazia rejeitada pela validacao de schema.");
        }
        JsonNode node;
        try {
            node = mapper.readTree(rawJson);
        } catch (Exception ex) {
            throw new InferenceException("Resposta nao-JSON rejeitada pela validacao de schema.", ex);
        }
        if (!node.isObject()) {
            throw new InferenceException("Resposta JSON deve ser um objeto.");
        }

        List<String> errors = new ArrayList<>();
        for (String field : REQUIRED) {
            if (!node.has(field)) {
                errors.add("campo obrigatorio ausente: " + field);
            }
        }

        requireEnum(node, "tipoSolicitacao", TIPOS, errors);
        requireEnum(node, "sentimento", SENTIMENTOS, errors);
        requireEnum(node, "prioridadeSugerida", PRIORIDADES, errors);
        requireText(node, "resumoOperacional", errors);
        requireText(node, "proximaAcao", errors);
        requireText(node, "justificativaIntervencencao", errors, true);
        if (node.has("intervencaoHumana") && !node.get("intervencaoHumana").isBoolean()) {
            errors.add("intervencaoHumana deve ser boolean");
        }
        if (node.has("nivelConfianca")) {
            JsonNode conf = node.get("nivelConfianca");
            if (!conf.isNumber() || conf.asDouble() < 0 || conf.asDouble() > 1) {
                errors.add("nivelConfianca deve ser number entre 0 e 1");
            }
        }
        requireStringOrNull(node, "codigoColeta", errors);
        requireStringOrNull(node, "cliente", errors);
        requireStringArray(node, "camposAusentesOuErro", errors);
        requireStringArray(node, "indicadoresFalha", errors);

        if (!errors.isEmpty()) {
            throw new InferenceException("JSON invalido/incompleto: " + String.join("; ", errors));
        }

        try {
            return mapper.treeToValue(node, TriageResult.class);
        } catch (Exception ex) {
            throw new InferenceException("Falha ao mapear JSON validado para TriageResult.", ex);
        }
    }

    private void ensureSchemaResourcePresent() {
        try (InputStream in = getClass().getResourceAsStream("/inference/triage-schema.json")) {
            if (in == null) {
                throw new IllegalStateException("Recurso /inference/triage-schema.json nao encontrado no classpath.");
            }
            mapper.readTree(in);
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao ler triage-schema.json: " + ex.getMessage(), ex);
        }
    }

    private static void requireEnum(JsonNode node, String field, Set<String> allowed, List<String> errors) {
        if (!node.has(field) || node.get(field).isNull()) {
            return;
        }
        String value = node.get(field).asText();
        if (!allowed.contains(value)) {
            errors.add(field + " invalido: " + value);
        }
    }

    private static void requireText(JsonNode node, String field, List<String> errors) {
        requireText(node, field, errors, false);
    }

    private static void requireText(JsonNode node, String field, List<String> errors, boolean allowEmpty) {
        if (!node.has(field) || node.get(field).isNull()) {
            return;
        }
        if (!node.get(field).isTextual()) {
            errors.add(field + " deve ser string");
            return;
        }
        if (!allowEmpty && node.get(field).asText().isBlank()) {
            errors.add(field + " nao pode ser vazio");
        }
    }

    private static void requireStringOrNull(JsonNode node, String field, List<String> errors) {
        if (!node.has(field)) {
            return;
        }
        JsonNode value = node.get(field);
        if (!(value.isNull() || value.isTextual())) {
            errors.add(field + " deve ser string ou null");
        }
    }

    private static void requireStringArray(JsonNode node, String field, List<String> errors) {
        if (!node.has(field)) {
            return;
        }
        JsonNode value = node.get(field);
        if (!value.isArray()) {
            errors.add(field + " deve ser array");
            return;
        }
        Iterator<JsonNode> it = value.elements();
        while (it.hasNext()) {
            JsonNode item = it.next();
            if (!item.isTextual()) {
                errors.add(field + " deve conter apenas strings");
                return;
            }
        }
    }
}
