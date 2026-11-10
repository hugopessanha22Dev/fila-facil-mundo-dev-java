package com.mundodev.filafacil.inference.prompt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mundodev.filafacil.error.InferenceException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class GroundingValidator {

    private static final Set<String> ORIGENS = Set.of("fato", "inferencia", "ausente");
    private final ObjectMapper mapper;

    public GroundingValidator() {
        this(new ObjectMapper());
    }

    public GroundingValidator(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public JsonNode validate(String rawJson, String sourceText) {
        if (rawJson == null || rawJson.isBlank()) {
            throw new InferenceException("Saida vazia rejeitada pela rubrica de formato.");
        }
        JsonNode node;
        try {
            node = mapper.readTree(rawJson);
        } catch (Exception ex) {
            throw new InferenceException("Rubrica formato: resposta nao-JSON.", ex);
        }
        if (!node.isObject()) {
            throw new InferenceException("Rubrica formato: JSON deve ser objeto.");
        }

        List<String> errors = new ArrayList<>();
        if (!node.has("proximaAcao") || node.get("proximaAcao").asText("").isBlank()) {
            errors.add("proximaAcao obrigatoria");
        }
        if (!node.has("tipoSolicitacao") || node.get("tipoSolicitacao").asText("").isBlank()) {
            errors.add("tipoSolicitacao obrigatorio");
        }

        String corpus = sourceText == null ? "" : sourceText.toLowerCase(Locale.ROOT);
        rejectInventedStatus(node, corpus, errors);
        validateCamposGrounding(node.path("campos"), corpus, errors);

        if (!errors.isEmpty()) {
            throw new InferenceException("Falha de grounding/rubrica: " + String.join("; ", errors));
        }
        return node;
    }

    private void rejectInventedStatus(JsonNode node, String corpus, List<String> errors) {
        String status = textOrNull(node, "status");
        if (status != null && !status.isBlank()) {
            String st = status.toLowerCase(Locale.ROOT);
            boolean mentioned = corpus.contains(st) || corpus.contains(st.replace('_', ' '));
            JsonNode camposStatus = node.path("campos").path("status");
            String origem = camposStatus.path("origem").asText("");
            if (!mentioned && !"ausente".equals(origem) && !"inferencia".equals(origem)) {
                errors.add("status inventado sem evidencia na mensagem/historico");
            }
            if (!mentioned && "fato".equals(origem)) {
                errors.add("status marcado como fato sem aparecer na fonte");
            }
        }
    }

    private void validateCamposGrounding(JsonNode campos, String corpus, List<String> errors) {
        if (campos == null || campos.isMissingNode() || campos.isNull()) {
            return;
        }
        if (!campos.isObject()) {
            errors.add("campos deve ser objeto");
            return;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = campos.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            JsonNode field = entry.getValue();
            String origem = field.path("origem").asText("");
            if (!origem.isBlank() && !ORIGENS.contains(origem)) {
                errors.add("origem invalida em " + entry.getKey());
            }
            String valor = field.path("valor").isNull() ? null : field.path("valor").asText(null);
            if ("fato".equals(origem) && valor != null && !valor.isBlank()) {
                String needle = valor.toLowerCase(Locale.ROOT);
                if (!corpus.contains(needle)) {
                    errors.add("campo " + entry.getKey() + " marcado como fato sem evidencia");
                }
            }
            if ("ausente".equals(origem) && valor != null && !valor.isBlank()) {
                errors.add("campo " + entry.getKey() + " ausente nao pode ter valor");
            }
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        if (!node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asText();
    }
}
