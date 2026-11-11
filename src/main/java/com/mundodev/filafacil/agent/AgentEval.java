package com.mundodev.filafacil.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mundodev.filafacil.inference.tools.MockOperationalTools;
import com.mundodev.filafacil.state.AppState;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class AgentEval {

    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public record Scenario(String id, String texto, boolean consultaIndisponivel, boolean erroEscrita, String parada) {
    }

    public record ScenarioResult(
            String id,
            boolean sucesso,
            int custoEstimado,
            long latenciaMs,
            int passos,
            List<String> ferramentas,
            int falhas,
            int escalonamentos,
            boolean aderencia,
            String parada,
            boolean bateu) {
    }

    public record Report(List<ScenarioResult> cenarios) {

        public boolean aprovado() {
            return !cenarios.isEmpty() && cenarios.stream().allMatch(ScenarioResult::bateu);
        }

        public String render() {
            StringBuilder out = new StringBuilder();
            out.append("sucesso custoEstimado latencia passos ferramentas falhas escalonamentos aderencia total=")
                    .append(cenarios.size()).append('\n');
            for (ScenarioResult item : cenarios) {
                out.append(item.id()).append(" sucesso=").append(item.sucesso())
                        .append(" custoEstimado=").append(item.custoEstimado())
                        .append(" latenciaMs=").append(item.latenciaMs())
                        .append(" passos=").append(item.passos())
                        .append(" ferramentas=").append(String.join("|", item.ferramentas()))
                        .append(" falhas=").append(item.falhas())
                        .append(" escalonamentos=").append(item.escalonamentos())
                        .append(" aderencia=").append(item.aderencia())
                        .append(" parada=").append(item.parada())
                        .append('\n');
            }
            return out.toString();
        }
    }

    public List<Scenario> scenarios() {
        try (InputStream in = AgentEval.class.getResourceAsStream("/agent/scenarios.json")) {
            if (in == null) {
                throw new IllegalStateException("scenarios.json ausente");
            }
            JsonNode root = MAPPER.readTree(in);
            List<Scenario> scenarios = new ArrayList<>();
            for (JsonNode node : root) {
                scenarios.add(new Scenario(
                        node.path("id").asText(),
                        node.path("texto").asText(),
                        node.path("consultaIndisponivel").asBoolean(false),
                        node.path("erroEscrita").asBoolean(false),
                        node.path("parada").asText()));
            }
            return List.copyOf(scenarios);
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao ler scenarios.json", ex);
        }
    }

    public Report evaluate(RagRules rules, AgentLimits limits) {
        List<ScenarioResult> results = new ArrayList<>();
        for (Scenario scenario : scenarios()) {
            AgentLoop loop = AgentLoop.create(rules, limits, new MockOperationalTools(new AppState()));
            AgentLoop.Run run = loop.run(scenario.texto(), scenario.consultaIndisponivel(), scenario.erroEscrita());
            results.add(new ScenarioResult(
                    scenario.id(),
                    run.sucesso(),
                    run.custoEstimado(),
                    run.latenciaMs(),
                    run.trace().size(),
                    run.acoes(),
                    run.falhas(),
                    run.escalonamentos(),
                    run.aderente(),
                    run.parada(),
                    scenario.parada().equals(run.parada())));
        }
        return new Report(List.copyOf(results));
    }

    public void write(Path file, Report report) throws IOException {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("total", report.cenarios().size());
        root.put("aprovado", report.aprovado());
        ArrayNode items = root.putArray("cenarios");
        for (ScenarioResult item : report.cenarios()) {
            ObjectNode node = items.addObject();
            node.put("id", item.id());
            node.put("sucesso", item.sucesso());
            node.put("custoEstimado", item.custoEstimado());
            node.put("latenciaMs", item.latenciaMs());
            node.put("passos", item.passos());
            node.put("ferramentas", String.join("|", item.ferramentas()));
            node.put("falhas", item.falhas());
            node.put("escalonamentos", item.escalonamentos());
            node.put("aderencia", item.aderencia());
            node.put("parada", item.parada());
        }
        Files.createDirectories(file.getParent());
        MAPPER.writeValue(file.toFile(), root);
    }
}
