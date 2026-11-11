package com.mundodev.filafacil.cloudai.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mundodev.filafacil.cloudai.Redactor;
import com.mundodev.filafacil.cloudai.functions.PromptCleaner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ArtifactStore {

    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private final Path envRoot;

    public ArtifactStore(Path root, String environment) {
        this.envRoot = root.resolve(environment);
    }

    public Path envRoot() {
        return envRoot;
    }

    public void writeDatasets(String domain, String raw, String processed) throws IOException {
        writeNew(envRoot.resolve("datasets").resolve("raw").resolve(domain).resolve("batch.json"), Redactor.mask(raw));
        writeNew(envRoot.resolve("datasets").resolve("processed").resolve(domain).resolve("batch.json"), Redactor.mask(processed));
    }

    public Path savePrompt(String id, int version, String text) throws IOException {
        Path file = envRoot.resolve("prompts").resolve(id).resolve("v" + version + ".txt");
        if (Files.exists(file)) {
            return file;
        }
        Files.createDirectories(file.getParent());
        Files.writeString(file, PromptCleaner.clean(text));
        return file;
    }

    public void saveEvaluation(String model, String date, String workload, String body) throws IOException {
        Path file = envRoot.resolve("evals").resolve(safe(model)).resolve(date).resolve(workload).resolve("report.json");
        writeNew(file, Redactor.mask(body));
    }

    public Path saveInference(String date, String id, ObjectNode body) throws IOException {
        body.put("id", id);
        maskTextFields(body);
        Path file = envRoot.resolve("inferences").resolve(date).resolve(id + ".json");
        Files.createDirectories(file.getParent());
        MAPPER.writeValue(file.toFile(), body);
        return file;
    }

    public long storageBytes() throws IOException {
        if (!Files.exists(envRoot)) {
            return 0;
        }
        try (var walk = Files.walk(envRoot)) {
            return walk.filter(Files::isRegularFile).mapToLong(path -> {
                try {
                    return Files.size(path);
                } catch (IOException ex) {
                    return 0;
                }
            }).sum();
        }
    }

    private static void writeNew(Path file, String body) throws IOException {
        Files.createDirectories(file.getParent());
        if (!Files.exists(file)) {
            Files.writeString(file, body);
        }
    }

    private static void maskTextFields(ObjectNode body) {
        List<String> names = new ArrayList<>();
        body.fieldNames().forEachRemaining(names::add);
        for (String name : names) {
            var node = body.get(name);
            if (node != null && node.isTextual()) {
                body.put(name, Redactor.mask(node.asText()));
            }
        }
    }

    private static String safe(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
