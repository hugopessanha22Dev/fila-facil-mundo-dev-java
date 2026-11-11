package com.mundodev.filafacil.aidevops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public final class ReleaseStore {

    private final ObjectMapper mapper = new ObjectMapper();

    public void writeEvaluation(Path root, ReleaseCombo combo, QualityReport report) throws IOException {
        Files.createDirectories(root);
        ObjectNode node = mapper.createObjectNode();
        node.put("approved", report.approved());
        combo.writeTo(node.putObject("combo"));
        node.put("acuracia", report.accuracy());
        node.put("extracao", report.extraction());
        node.put("jsonValido", report.validJson());
        node.put("schema", report.schemaAdherence());
        node.put("resumo", report.summary());
        node.put("custo", report.costUsd());
        node.put("latencia", report.latencyMs());
        node.put("regressao", report.regression());
        ObjectNode failures = node.putObject("falhasPorCategoria");
        report.failuresByCategory().forEach(failures::put);
        ArrayNode stages = node.putArray("stages");
        for (QualityReport.Stage stage : report.stages()) {
            ObjectNode item = stages.addObject();
            item.put("name", stage.name());
            item.put("approved", stage.approved());
        }
        mapper.writerWithDefaultPrettyPrinter().writeValue(root.resolve("last-eval.json").toFile(), node);

        Path pkg = root.resolve("package").resolve("release.json");
        if (report.approved()) {
            Files.createDirectories(pkg.getParent());
            ObjectNode release = mapper.createObjectNode();
            combo.writeTo(release);
            release.put("approved", true);
            mapper.writerWithDefaultPrettyPrinter().writeValue(pkg.toFile(), release);
        } else if (Files.exists(pkg)) {
            Files.delete(pkg);
        }
    }

    public int promote(Path root, String env, ReleaseCombo current) throws IOException {
        if (!allowed(env)) {
            System.out.println("Recusado: ambiente invalido");
            return 1;
        }
        Path eval = root.resolve("last-eval.json");
        if (!Files.exists(eval)) {
            System.out.println("Recusado: avaliacao offline ausente");
            return 1;
        }
        JsonNode node = mapper.readTree(eval.toFile());
        if (!node.path("approved").asBoolean(false)) {
            System.out.println("Recusado: avaliacao offline nao aprovada");
            return 1;
        }
        if (!current.sameAs(node.path("combo"))) {
            System.out.println("Recusado: combinacao diferente da avaliada");
            return 1;
        }
        Path envDir = root.resolve(env);
        Files.createDirectories(envDir);
        Path active = envDir.resolve("active.json");
        Path previous = envDir.resolve("previous.json");
        if (Files.exists(active)) {
            Files.copy(active, previous, StandardCopyOption.REPLACE_EXISTING);
        }
        ObjectNode written = mapper.createObjectNode();
        current.writeTo(written);
        mapper.writerWithDefaultPrettyPrinter().writeValue(active.toFile(), written);
        System.out.println("promovido env=" + env + " promptVersion=" + current.promptVersion());
        return 0;
    }

    public int rollback(Path root, String env) throws IOException {
        if (!allowed(env)) {
            System.out.println("Recusado: ambiente invalido");
            return 1;
        }
        Path previous = root.resolve(env).resolve("previous.json");
        Path active = root.resolve(env).resolve("active.json");
        if (!Files.exists(previous)) {
            System.out.println("Recusado: nao ha versao anterior");
            return 1;
        }
        Files.copy(previous, active, StandardCopyOption.REPLACE_EXISTING);
        Files.delete(previous);
        System.out.println("rollback env=" + env);
        return 0;
    }

    public String status(Path root) throws IOException {
        StringBuilder out = new StringBuilder();
        for (String env : List.of("homolog", "prod")) {
            Path active = root.resolve(env).resolve("active.json");
            if (!Files.exists(active)) {
                out.append(env).append("=vazio\n");
                continue;
            }
            JsonNode node = mapper.readTree(active.toFile());
            out.append(env)
                    .append(" promptVersion=")
                    .append(node.path("promptVersion").asText())
                    .append('\n');
        }
        return out.toString();
    }

    private static boolean allowed(String env) {
        return "homolog".equals(env) || "prod".equals(env);
    }
}
