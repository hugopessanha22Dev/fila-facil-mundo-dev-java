package com.mundodev.filafacil.inference.prompt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mundodev.filafacil.error.InferenceException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class FewShotCatalog {

    public static final Set<String> REQUIRED_TYPES = Set.of(
            "positivo", "negativo", "contraexemplo", "escalacao");

    private final List<FewShotExample> examples;

    public FewShotCatalog() {
        this(loadDefaults(new ObjectMapper()));
    }

    public FewShotCatalog(List<FewShotExample> examples) {
        this.examples = List.copyOf(examples);
        Set<String> types = this.examples.stream()
                .map(e -> e.type().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        for (String required : REQUIRED_TYPES) {
            if (!types.contains(required)) {
                throw new InferenceException("Few-shot catalog sem tipo: " + required);
            }
        }
    }

    public List<FewShotExample> all() {
        return examples;
    }

    public List<FewShotExample> forTask(String taskId) {
        return examples.stream().filter(e -> taskId.equals(e.taskId())).toList();
    }

    public List<FewShotExample> relevantFor(String taskId) {
        List<FewShotExample> task = forTask(taskId);
        if (!task.isEmpty()) {
            return task;
        }
        // fallback: um de cada tipo
        List<FewShotExample> picked = new ArrayList<>();
        for (String type : REQUIRED_TYPES) {
            examples.stream()
                    .filter(e -> type.equalsIgnoreCase(e.type()))
                    .findFirst()
                    .ifPresent(picked::add);
        }
        return picked;
    }

    public String formatBlock(String taskId) {
        StringBuilder sb = new StringBuilder();
        sb.append("Exemplos few-shot:\n");
        for (FewShotExample ex : relevantFor(taskId)) {
            sb.append("- tipo=").append(ex.type())
                    .append(" input=").append(ex.input())
                    .append(" output=").append(ex.output())
                    .append('\n');
        }
        return sb.toString();
    }

    private static List<FewShotExample> loadDefaults(ObjectMapper mapper) {
        String path = "/inference/fewshot/catalog.json";
        try (InputStream in = FewShotCatalog.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new InferenceException("Resource few-shot nao encontrado: " + path);
            }
            JsonNode root = mapper.readTree(in);
            List<FewShotExample> list = new ArrayList<>();
            for (JsonNode node : root.path("examples")) {
                list.add(mapper.treeToValue(node, FewShotExample.class));
            }
            return list;
        } catch (InferenceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InferenceException("Falha ao carregar few-shot catalog: " + ex.getMessage(), ex);
        }
    }
}
