package com.mundodev.filafacil.aidevops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class DeliveryCatalog {

    private static final Set<String> FAMILIES = Set.of("consulta", "ocorrencia", "reclamacao");
    private static final List<String> GOLDEN_FILES = List.of(
            "/aidevops/golden/consulta.json",
            "/aidevops/golden/ocorrencia.json",
            "/aidevops/golden/reclamacao.json");

    private final ObjectMapper mapper = new ObjectMapper();

    public ReleaseCombo combo() {
        return ReleaseCombo.from(readJson("/aidevops/release-combo.json"));
    }

    public ModelSettings settings(String env) {
        JsonNode root = readJson("/aidevops/model-config.json");
        JsonNode node = root.path(env);
        if (node.isMissingNode() || node.isNull()) {
            throw new IllegalArgumentException("Ambiente sem configuracao de modelo: " + env);
        }
        return new ModelSettings(
                env,
                node.path("primaryModel").asText(),
                node.path("fallbackModel").asText(),
                node.path("maxRetries").asInt(),
                node.path("rateLimitPerMinute").asInt());
    }

    public QualityGates gates() {
        JsonNode node = readJson("/aidevops/quality-gates.json");
        return new QualityGates(
                node.path("minValidJson").asDouble(),
                node.path("minSchemaAdherence").asDouble(),
                node.path("minAccuracy").asDouble(),
                node.path("maxCostUsd").asDouble(),
                node.path("maxLatencyMs").asDouble(),
                node.path("maxAccuracyDrop").asDouble(),
                node.path("maxExtractionDrop").asDouble());
    }

    public BaselineMetrics baseline() {
        JsonNode node = readJson("/aidevops/baseline.json");
        return new BaselineMetrics(
                node.path("accuracy").asDouble(),
                node.path("extraction").asDouble(),
                node.path("validJson").asDouble(),
                node.path("schemaAdherence").asDouble(),
                node.path("summary").asDouble(),
                node.path("costUsd").asDouble(),
                node.path("latencyMs").asDouble());
    }

    public String prompt() {
        return readText("/aidevops/prompt-triage-v1.txt");
    }

    public List<GoldenCase> golden() {
        List<GoldenCase> cases = new ArrayList<>();
        for (String path : GOLDEN_FILES) {
            JsonNode node = readJson(path);
            String category = node.path("category").asText();
            if (!FAMILIES.contains(category)) {
                throw new IllegalStateException("Familia desconhecida no golden: " + category);
            }
            cases.add(new GoldenCase(
                    node.path("id").asText(),
                    category,
                    node.path("message").asText(),
                    node.path("expectedTipo").asText(),
                    node.path("expectedPrioridade").asText()));
        }
        return List.copyOf(cases);
    }

    private JsonNode readJson(String path) {
        try (InputStream in = DeliveryCatalog.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Recurso ausente: " + path);
            }
            return mapper.readTree(in);
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao ler " + path, ex);
        }
    }

    private String readText(String path) {
        try (InputStream in = DeliveryCatalog.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Recurso ausente: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao ler " + path, ex);
        }
    }
}
