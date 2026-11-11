package com.mundodev.filafacil.aidevops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mundodev.filafacil.inference.InferenceGateway;
import com.mundodev.filafacil.inference.InferenceResponse;
import com.mundodev.filafacil.inference.MockInferenceGateway;
import com.mundodev.filafacil.inference.ModelProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiDeliveryTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final DeliveryCatalog catalog = new DeliveryCatalog();

    @TempDir
    Path temp;

    @Test
    void prodEMaisRestritoQueDevERecursosNaoTemChave() throws Exception {
        ModelSettings dev = catalog.settings("dev");
        ModelSettings prod = catalog.settings("prod");
        assertTrue(prod.maxRetries() <= dev.maxRetries());
        assertTrue(prod.rateLimitPerMinute() <= dev.rateLimitPerMinute());
        assertTrue(prod.maxRetries() < dev.maxRetries());
        try (var files = Files.walk(Path.of("src/main/resources/aidevops"))) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                String text = Files.readString(file);
                assertFalse(text.contains("gsk_"), file.toString());
                assertFalse(text.contains("sk-"), file.toString());
                assertFalse(text.contains("AQ."), file.toString());
            }
        }
    }

    @Test
    void goldenTemAsTresFamiliasENaoMexeNoEvalAntigo() throws Exception {
        Set<String> families = catalog.golden().stream()
                .map(GoldenCase::category)
                .collect(Collectors.toSet());
        assertEquals(Set.of("consulta", "ocorrencia", "reclamacao"), families);
        try (var files = Files.list(Path.of("src/test/resources/inference/golden"))) {
            long json = files.filter(path -> path.getFileName().toString().endsWith(".json")).count();
            assertEquals(3, json);
        }
    }

    @Test
    void promptESchemaAceitamContratoERecusamSegredoOuTextoLivre() {
        assertTrue(PromptLint.check(catalog.prompt()).ok());
        String leaked = catalog.prompt() + " gsk_abcDEF123";
        assertFalse(PromptLint.check(leaked).ok());
        assertFalse(Redacted.keepsSecret(leaked));

        OutputSchema.Parsed valid = OutputSchema.parse(
                "{\"tipoSolicitacao\":\"consulta_entrega\",\"resumo\":\"ok\",\"proximaAcao\":\"ver\",\"prioridade\":\"baixa\"}");
        assertTrue(valid.jsonValid());
        assertTrue(valid.schemaOk());

        OutputSchema.Parsed free = OutputSchema.parse("texto livre sem json");
        assertFalse(free.jsonValid());
        assertFalse(free.schemaOk());
    }

    @Test
    void entradaVaziaNaoChamaModelo() {
        AtomicInteger calls = new AtomicInteger();
        InferenceGateway gateway = request -> {
            calls.incrementAndGet();
            throw new IllegalStateException("modelo nao deveria ser chamado");
        };
        GoldenCase empty = new GoldenCase("vazio", "consulta", "  ", "consulta_entrega", "baixa");
        CaseOutcome outcome = new GuardedInference().run(
                empty, catalog.settings("dev"), gateway, catalog.prompt(), "v1", "v1", 10);
        assertTrue(outcome.inputRejected());
        assertEquals(0, calls.get());
        assertEquals(0, outcome.modelCalls());
    }

    @Test
    void mockAprovaELatenciaDentroDoTetoNaoReprova() throws Exception {
        InferenceGateway slow = request -> {
            InferenceResponse response = new MockInferenceGateway().complete(request);
            return new InferenceResponse(
                    response.rawJson(),
                    response.profileUsed(),
                    response.modelId(),
                    2000,
                    response.estimatedCostUsd(),
                    false);
        };
        QualityReport report = new OfflineEvaluation().run(temp, "dev", slow);
        assertEquals(3, report.total());
        assertEquals(1.0, report.accuracy(), 0.001);
        assertEquals(1.0, report.extraction(), 0.001);
        assertEquals(1.0, report.validJson(), 0.001);
        assertEquals(1.0, report.schemaAdherence(), 0.001);
        assertEquals(1.0, report.summary(), 0.001);
        assertTrue(report.latencyMs() > catalog.baseline().latencyMs());
        assertTrue(report.latencyMs() < catalog.gates().maxLatencyMs());
        assertTrue(report.costUsd() <= catalog.gates().maxCostUsd());
        assertFalse(report.regression());
        assertTrue(report.approved());
        assertEquals(0, report.failuresByCategory().get("consulta"));
        JsonNode saved = mapper.readTree(temp.resolve("last-eval.json").toFile());
        assertTrue(saved.has("acuracia"));
        assertTrue(saved.has("extracao"));
        assertTrue(saved.has("jsonValido"));
        assertTrue(saved.has("schema"));
        assertTrue(saved.has("resumo"));
        assertTrue(saved.has("custo"));
        assertTrue(saved.has("latencia"));
        assertTrue(saved.has("regressao"));
        assertTrue(saved.has("falhasPorCategoria"));
        assertTrue(Files.exists(temp.resolve("package/release.json")));
        String trace = Files.readString(temp.resolve("traces/consulta-1.json"));
        assertTrue(trace.contains("\"promptVersion\""));
        assertTrue(trace.contains("\"fallback\" : false") || trace.contains("\"fallback\" : false"));
        assertFalse(trace.contains("gsk_"));
    }

    @Test
    void quedaDeAcuraciaReprovaENaoPublicaPacote() throws Exception {
        InferenceGateway wrong = request -> new InferenceResponse(
                "{\"tipoSolicitacao\":\"consulta_entrega\",\"resumo\":\"x\",\"proximaAcao\":\"y\",\"prioridade\":\"baixa\"}",
                ModelProfile.LIGHT,
                "mock-light",
                5,
                0.001,
                false);
        QualityReport report = new OfflineEvaluation().run(temp, "dev", wrong);
        assertTrue(report.accuracy() < 0.8);
        assertTrue(report.regression());
        assertFalse(report.approved());
        assertFalse(Files.exists(temp.resolve("package/release.json")));
        assertTrue(report.failuresByCategory().get("ocorrencia") > 0);
        assertTrue(report.failuresByCategory().get("reclamacao") > 0);
    }

    @Test
    void traceMascaraChaveNaResposta() throws Exception {
        InferenceGateway leak = request -> new InferenceResponse(
                "{\"tipoSolicitacao\":\"consulta_entrega\",\"resumo\":\"token gsk_SuperSecreta123\",\"proximaAcao\":\"ver\",\"prioridade\":\"baixa\"}",
                ModelProfile.LIGHT,
                "mock-light",
                4,
                0.001,
                false);
        new OfflineEvaluation().run(temp, "dev", leak);
        String trace = Files.readString(temp.resolve("traces/consulta-1.json"));
        assertFalse(trace.contains("gsk_"));
        assertTrue(trace.contains("***"));
    }

    @Test
    void fallbackRecuperaEDuplaFalhaVaiParaHumano() {
        AtomicInteger recovered = new AtomicInteger();
        InferenceGateway recover = request -> {
            recovered.incrementAndGet();
            if ("mock-detailed".equals(request.modelId())) {
                return new InferenceResponse(
                        "{\"tipoSolicitacao\":\"consulta_entrega\",\"resumo\":\"ok\",\"proximaAcao\":\"ver\",\"prioridade\":\"baixa\"}",
                        ModelProfile.DETAILED,
                        request.modelId(),
                        4,
                        0.001,
                        true);
            }
            return new InferenceResponse("texto livre", ModelProfile.LIGHT, request.modelId(), 3, 0.001, false);
        };
        GoldenCase consulta = catalog.golden().get(0);
        CaseOutcome ok = new GuardedInference().run(
                consulta, catalog.settings("prod"), recover, catalog.prompt(), "v1", "v1", 10);
        assertEquals(2, recovered.get());
        assertTrue(ok.fallback());
        assertFalse(ok.humanIntervention());
        assertTrue(ok.accepted());
        assertTrue(ok.error().contains("schema"));
        assertEquals(0, ok.retries());

        InferenceGateway bothFail = request -> new InferenceResponse(
                "texto livre", request.profile(), request.modelId(), 2, 0, false);
        CaseOutcome human = new GuardedInference().run(
                consulta, catalog.settings("prod"), bothFail, catalog.prompt(), "v1", "v1", 10);
        assertTrue(human.fallback());
        assertTrue(human.humanIntervention());
        assertFalse(human.accepted());
        assertEquals(2, human.modelCalls());
    }

    @Test
    void promoteBloqueadoAceitoERollback() throws Exception {
        InferenceGateway bad = request -> new InferenceResponse(
                "texto livre", ModelProfile.LIGHT, "mock-light", 1, 0, false);
        assertEquals(1, AiDeliveryMain.execute(new String[]{"evaluate"}, Map.of(), temp, bad));
        assertEquals(1, AiDeliveryMain.execute(new String[]{"promote", "prod"}, Map.of(), temp, bad));
        assertFalse(Files.exists(temp.resolve("prod/active.json")));

        Files.createDirectories(temp.resolve("homolog"));
        Files.writeString(temp.resolve("homolog/active.json"), """
                {"promptVersion":"v0","datasetVersion":"v0","embeddingVersion":"v0","schemaVersion":"v0","modelConfigVersion":"v0"}
                """);
        assertEquals(0, AiDeliveryMain.execute(new String[]{"evaluate"}, Map.of(), temp));
        assertEquals(0, AiDeliveryMain.execute(new String[]{"promote", "homolog"}, Map.of(), temp));
        JsonNode active = mapper.readTree(temp.resolve("homolog/active.json").toFile());
        assertEquals("v1", active.path("promptVersion").asText());
        assertEquals(0, AiDeliveryMain.execute(new String[]{"rollback", "homolog"}, Map.of(), temp));
        JsonNode restored = mapper.readTree(temp.resolve("homolog/active.json").toFile());
        assertEquals("v0", restored.path("promptVersion").asText());
        assertFalse(Files.exists(temp.resolve("homolog/previous.json")));
        String status = Files.readString(Path.of("src/main/java/com/mundodev/filafacil/controller/MenuController.java"));
        assertTrue(status.contains("case \"12\""));
        assertFalse(status.contains("case \"13\""));
        assertFalse(status.contains("aidevops"));
    }

    @Test
    void ciEReadmeApontamParaOEntrypointGratuito() throws Exception {
        String pom = Files.readString(Path.of("pom.xml"));
        assertTrue(pom.contains("com.mundodev.filafacil.aidevops.AiDeliveryMain"));
        assertTrue(pom.contains("<id>ai-delivery</id>"));

        String ci = Files.readString(Path.of(".github/workflows/ci.yml"));
        assertTrue(ci.indexOf("mvn -B test") < ci.indexOf("exec:java@ai-delivery"));
        assertTrue(ci.contains("exec:java@manifest-cli"));
        assertTrue(ci.contains("filafacil-manifest-validator.zip"));
        assertFalse(ci.contains("continue-on-error"));

        String readme = Files.readString(Path.of("README.md"));
        assertTrue(readme.contains("exec:java@ai-delivery"));
        assertTrue(readme.contains("-Dexec.args=evaluate"));
        assertTrue(readme.contains("mock"));
    }

    private static final class Redacted {
        private static boolean keepsSecret(String text) {
            return com.mundodev.filafacil.cloudai.Redactor.mask(text).contains("gsk_");
        }
    }
}
