package com.mundodev.filafacil.inference.prompt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mundodev.filafacil.error.InferenceException;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PromptLibrary {

    public static final List<String> REQUIRED_IDS = List.of(
            "classify", "extract", "summarize", "internal-reply", "transform");

    private final Map<String, PromptDefinition> prompts;

    public PromptLibrary() {
        this(loadDefaults(new ObjectMapper()));
    }

    public PromptLibrary(Map<String, PromptDefinition> prompts) {
        this.prompts = Map.copyOf(prompts);
        for (String id : REQUIRED_IDS) {
            if (!this.prompts.containsKey(id)) {
                throw new InferenceException("Prompt obrigatorio ausente na biblioteca: " + id);
            }
        }
    }

    public PromptDefinition get(String id) {
        PromptDefinition def = prompts.get(id);
        if (def == null) {
            throw new InferenceException("Prompt nao encontrado: " + id);
        }
        return def;
    }

    public Set<String> ids() {
        return prompts.keySet();
    }

    public int size() {
        return prompts.size();
    }

    private static Map<String, PromptDefinition> loadDefaults(ObjectMapper mapper) {
        Map<String, PromptDefinition> map = new LinkedHashMap<>();
        for (String id : REQUIRED_IDS) {
            String path = "/inference/prompts/" + id + ".json";
            try (InputStream in = PromptLibrary.class.getResourceAsStream(path)) {
                if (in == null) {
                    throw new InferenceException("Resource de prompt nao encontrado: " + path);
                }
                PromptDefinition def = mapper.readValue(in, PromptDefinition.class);
                validateStructure(def);
                map.put(def.id(), def);
            } catch (InferenceException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new InferenceException("Falha ao carregar prompt " + id + ": " + ex.getMessage(), ex);
            }
        }
        return map;
    }

    static void validateStructure(PromptDefinition def) {
        if (isBlank(def.id()) || isBlank(def.objective()) || isBlank(def.context())
                || isBlank(def.inputExpected()) || isBlank(def.outputFormat())
                || isBlank(def.systemTemplate())) {
            throw new InferenceException("Prompt com estrutura incompleta: " + def.id());
        }
        if (def.constraints() == null || def.constraints().isEmpty()) {
            throw new InferenceException("Prompt sem constraints: " + def.id());
        }
        if (def.qualityCriteria() == null || def.qualityCriteria().isEmpty()) {
            throw new InferenceException("Prompt sem qualityCriteria: " + def.id());
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
