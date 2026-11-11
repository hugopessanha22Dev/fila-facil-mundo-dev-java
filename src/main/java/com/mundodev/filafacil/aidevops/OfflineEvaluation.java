package com.mundodev.filafacil.aidevops;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mundodev.filafacil.cloudai.Redactor;
import com.mundodev.filafacil.inference.InferenceGateway;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OfflineEvaluation {

    private final ObjectMapper mapper = new ObjectMapper();
    private final GuardedInference guard = new GuardedInference();

    public QualityReport run(Path root, String env, InferenceGateway gateway) throws IOException {
        return run(root, env, gateway, new DeliveryCatalog(), new ReleaseStore());
    }

    public QualityReport run(
            Path root,
            String env,
            InferenceGateway gateway,
            DeliveryCatalog catalog,
            ReleaseStore store) throws IOException {
        ReleaseCombo combo = catalog.combo();
        PromptLint lint = PromptLint.check(catalog.prompt());
        List<CaseOutcome> outcomes = new ArrayList<>();
        if (lint.ok()) {
            ModelSettings settings = catalog.settings(env);
            int spent = 0;
            for (GoldenCase caso : catalog.golden()) {
                int budget = Math.max(0, settings.rateLimitPerMinute() - spent);
                CaseOutcome outcome = guard.run(
                        caso,
                        settings,
                        gateway,
                        catalog.prompt(),
                        combo.promptVersion(),
                        combo.modelConfigVersion(),
                        budget);
                outcomes.add(outcome);
                spent += outcome.modelCalls();
                writeTrace(root, outcome);
            }
        }

        QualityReport report = score(outcomes, catalog.golden().size(), lint.ok(), catalog.gates(), catalog.baseline());
        store.writeEvaluation(root, combo, report);
        return report;
    }

    private QualityReport score(
            List<CaseOutcome> outcomes,
            int expected,
            boolean promptOk,
            QualityGates gates,
            BaselineMetrics baseline) {
        int total = outcomes.size();
        int tipoHits = 0;
        int extractHits = 0;
        int jsonHits = 0;
        int schemaHits = 0;
        int summaryHits = 0;
        double cost = 0;
        long latencySum = 0;
        int latencyCases = 0;
        Map<String, Integer> failures = new LinkedHashMap<>();
        failures.put("consulta", 0);
        failures.put("ocorrencia", 0);
        failures.put("reclamacao", 0);
        for (CaseOutcome outcome : outcomes) {
            if (outcome.tipoMatch()) {
                tipoHits++;
                extractHits++;
            }
            if (outcome.prioridadeMatch()) {
                extractHits++;
            }
            if (outcome.jsonValid()) {
                jsonHits++;
            }
            if (outcome.schemaOk()) {
                schemaHits++;
            }
            if (outcome.summaryPresent()) {
                summaryHits++;
            }
            cost += outcome.costUsd();
            if (outcome.modelCalls() > 0) {
                latencySum += outcome.latencyMs();
                latencyCases++;
            }
            if (!outcome.accepted() || !outcome.tipoMatch()) {
                failures.merge(outcome.category(), 1, Integer::sum);
            }
        }
        double accuracy = ratio(tipoHits, total);
        double extraction = total == 0 ? 0 : extractHits / (double) (total * 2);
        double validJson = ratio(jsonHits, total);
        double schema = ratio(schemaHits, total);
        double summary = ratio(summaryHits, total);
        double latency = latencyCases == 0 ? 0 : latencySum / (double) latencyCases;
        boolean regression = (baseline.accuracy() - accuracy) > gates.maxAccuracyDrop()
                || (baseline.extraction() - extraction) > gates.maxExtractionDrop();
        boolean quality = accuracy >= gates.minAccuracy()
                && validJson >= gates.minValidJson()
                && schema >= gates.minSchemaAdherence()
                && cost <= gates.maxCostUsd()
                && latency <= gates.maxLatencyMs();
        boolean testsOk = promptOk && total == expected && expected > 0;
        boolean schemaStage = testsOk && schema >= gates.minSchemaAdherence();
        boolean evalStage = testsOk && quality;
        boolean regressionStage = testsOk && !regression;
        boolean approved = promptOk && testsOk && schemaStage && evalStage && regressionStage;
        List<QualityReport.Stage> stages = List.of(
                new QualityReport.Stage("prompt", promptOk),
                new QualityReport.Stage("testes", testsOk),
                new QualityReport.Stage("schema", schemaStage),
                new QualityReport.Stage("avaliacao", evalStage),
                new QualityReport.Stage("regressao", regressionStage),
                new QualityReport.Stage("pacote", approved));
        return new QualityReport(
                total,
                accuracy,
                extraction,
                validJson,
                schema,
                summary,
                cost,
                latency,
                regression,
                Map.copyOf(failures),
                approved,
                stages);
    }

    private void writeTrace(Path root, CaseOutcome outcome) throws IOException {
        Path dir = root.resolve("traces");
        Files.createDirectories(dir);
        ObjectNode node = mapper.createObjectNode();
        node.put("id", outcome.id());
        node.put("promptVersion", outcome.promptVersion());
        node.put("model", outcome.model());
        node.put("configVersion", outcome.configVersion());
        node.put("latencyMs", outcome.latencyMs());
        node.put("costUsd", outcome.costUsd());
        node.put("tokens", outcome.tokens());
        node.put("retries", outcome.retries());
        node.put("fallback", outcome.fallback());
        node.put("error", Redactor.mask(outcome.error()));
        node.put("response", outcome.redactedResponse());
        mapper.writerWithDefaultPrettyPrinter().writeValue(dir.resolve(outcome.id() + ".json").toFile(), node);
    }

    private static double ratio(int hits, int total) {
        if (total == 0) {
            return 0;
        }
        return hits / (double) total;
    }
}
