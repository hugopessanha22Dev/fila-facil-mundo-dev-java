package com.mundodev.filafacil.inference.eval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mundodev.filafacil.inference.FallbackInferenceGateway;
import com.mundodev.filafacil.inference.InferenceConfig;
import com.mundodev.filafacil.inference.InferenceGateway;
import com.mundodev.filafacil.inference.InferenceRequest;
import com.mundodev.filafacil.inference.InferenceResponse;
import com.mundodev.filafacil.inference.MockInferenceGateway;
import com.mundodev.filafacil.inference.ModelRouter;
import com.mundodev.filafacil.inference.TriageResult;
import com.mundodev.filafacil.inference.TriageSchemaValidator;

import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Avaliacao offline do golden dataset (modo mock por padrao).
 * mvn -q exec:java -Dexec.mainClass=com.mundodev.filafacil.inference.eval.OfflineEvalRunner
 */
public class OfflineEvalRunner {

    public static void main(String[] args) throws Exception {
        EvalReport report = runFromClasspath();
        System.out.println(report.summary());
        if (report.failures() > 0) {
            System.exit(1);
        }
    }

    public static EvalReport runFromClasspath() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        List<JsonNode> cases = loadGoldenCases(mapper);
        InferenceConfig config = InferenceConfig.fromEnvironment();
        InferenceGateway gateway = new FallbackInferenceGateway(new MockInferenceGateway(), config);
        ModelRouter router = new ModelRouter(config);
        TriageSchemaValidator validator = new TriageSchemaValidator();

        int total = 0;
        int jsonValido = 0;
        int camposOk = 0;
        int fallbacks = 0;
        long latencySum = 0;
        double costSum = 0;
        List<String> falhas = new ArrayList<>();

        for (JsonNode caso : cases) {
            total++;
            String id = caso.path("id").asText("case-" + total);
            String input = caso.path("input").asText();
            JsonNode expected = caso.path("expected");
            try {
                InferenceRequest request = router.route(input);
                InferenceResponse response = gateway.complete(request);
                TriageResult result = validator.validateAndParse(response.rawJson());
                jsonValido++;
                latencySum += response.latencyMs();
                costSum += response.estimatedCostUsd();
                if (response.usedFallback()) {
                    fallbacks++;
                }
                if (matchesExpected(result, expected)) {
                    camposOk++;
                } else {
                    falhas.add(id + ": campos divergentes do expected (tipo/prioridade/intervencao)");
                }
            } catch (Exception ex) {
                falhas.add(id + ": " + ex.getMessage());
            }
        }

        return new EvalReport(total, jsonValido, camposOk, fallbacks, latencySum, costSum, falhas);
    }

    private static List<JsonNode> loadGoldenCases(ObjectMapper mapper) throws Exception {
        List<JsonNode> cases = new ArrayList<>();
        try (InputStream index = OfflineEvalRunner.class.getResourceAsStream("/inference/golden/consulta.json")) {
            if (index == null) {
                throw new IllegalStateException("Golden dataset nao encontrado em /inference/golden/");
            }
        }
        for (String name : List.of("consulta.json", "ocorrencia.json", "reclamacao.json")) {
            try (InputStream in = OfflineEvalRunner.class.getResourceAsStream("/inference/golden/" + name)) {
                if (in != null) {
                    cases.add(mapper.readTree(in));
                }
            }
        }
        if (cases.isEmpty()) {
            Path dir = Path.of("src/test/resources/inference/golden");
            if (Files.isDirectory(dir)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.json")) {
                    for (Path path : stream) {
                        cases.add(mapper.readTree(path.toFile()));
                    }
                }
            }
        }
        if (cases.isEmpty()) {
            throw new IllegalStateException("Nenhum caso golden carregado.");
        }
        return cases;
    }

    private static boolean matchesExpected(TriageResult result, JsonNode expected) {
        if (expected == null || expected.isMissingNode()) {
            return true;
        }
        if (expected.has("tipoSolicitacao")
                && !expected.get("tipoSolicitacao").asText().equals(result.tipoSolicitacao())) {
            return false;
        }
        if (expected.has("prioridadeSugerida")
                && !expected.get("prioridadeSugerida").asText().equals(result.prioridadeSugerida())) {
            return false;
        }
        if (expected.has("intervencaoHumana")
                && expected.get("intervencaoHumana").asBoolean() != result.intervencaoHumana()) {
            return false;
        }
        return true;
    }

    public record EvalReport(
            int total,
            int jsonValido,
            int camposOk,
            int fallbacks,
            long latencySumMs,
            double costSumUsd,
            List<String> falhasDetalhe) {

        public int failures() {
            return total - camposOk;
        }

        public String summary() {
            double jsonRate = total == 0 ? 0 : (100.0 * jsonValido / total);
            double fieldRate = total == 0 ? 0 : (100.0 * camposOk / total);
            double fallbackRate = total == 0 ? 0 : (100.0 * fallbacks / total);
            double avgLatency = total == 0 ? 0 : (latencySumMs * 1.0 / total);
            StringBuilder sb = new StringBuilder();
            sb.append("=== Offline Inference Eval ===\n");
            sb.append("total=").append(total)
                    .append(" jsonValido%=").append(round(jsonRate))
                    .append(" camposOk%=").append(round(fieldRate))
                    .append(" fallback%=").append(round(fallbackRate))
                    .append(" avgLatencyMs=").append(round(avgLatency))
                    .append(" costUsd~=").append(round(costSumUsd))
                    .append('\n');
            if (!falhasDetalhe.isEmpty()) {
                sb.append("Falhas:\n");
                for (String f : falhasDetalhe) {
                    sb.append(" - ").append(f).append('\n');
                }
                sb.append("Ajuste sugerido: revisar heuristica do mock/router ou expected do golden.\n");
            }
            return sb.toString();
        }

        private static double round(double v) {
            return Math.round(v * 100.0) / 100.0;
        }
    }
}
