package com.mundodev.filafacil.inference.prompt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mundodev.filafacil.error.InferenceException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class PromptComposer {

    private final PromptLibrary library;
    private final FewShotCatalog catalog;
    private final List<String> regrasGlobais;

    public PromptComposer(PromptLibrary library, FewShotCatalog catalog) {
        this(library, catalog, carregarGlobais());
    }

    PromptComposer(PromptLibrary library, FewShotCatalog catalog, List<String> regrasGlobais) {
        this.library = library;
        this.catalog = catalog;
        this.regrasGlobais = List.copyOf(regrasGlobais);
    }

    public String composeSystemPrompt(String taskId, String sessionSummary) {
        return compose(taskId, sessionSummary, null);
    }

    public String compose(String taskId, String sessionSummary, String entrada) {
        PromptDefinition def = library.get(taskId);
        StringBuilder sb = new StringBuilder();
        sb.append("Regras globais:\n");
        for (String regra : regrasGlobais) {
            sb.append("- ").append(regra).append('\n');
        }
        sb.append("Regras da tarefa:\n");
        for (String regra : def.constraints()) {
            sb.append("- ").append(regra).append('\n');
        }
        sb.append("Regras de formato:\n");
        sb.append(def.outputFormat()).append('\n');
        sb.append(def.systemTemplate()).append('\n');
        sb.append("Objetivo: ").append(def.objective()).append('\n');
        sb.append("Contexto: ").append(def.context()).append('\n');
        sb.append("Restricoes: ").append(String.join("; ", def.constraints())).append('\n');
        sb.append("Criterios: ").append(String.join("; ", def.qualityCriteria())).append('\n');
        sb.append("Formato: ").append(def.outputFormat()).append('\n');
        sb.append(catalog.formatBlock(taskId));
        if (sessionSummary != null && !sessionSummary.isBlank()) {
            sb.append("Memoria da sessao:\n").append(sessionSummary).append('\n');
        }
        if (entrada != null) {
            sb.append("<<<ENTRADA>>>\n").append(entrada).append("\n<<<FIM>>>\n");
        }
        return sb.toString();
    }

    private static List<String> carregarGlobais() {
        try (InputStream in = PromptComposer.class.getResourceAsStream("/inference/prompts/camadas.json")) {
            if (in == null) {
                throw new InferenceException("camadas.json ausente");
            }
            JsonNode node = new ObjectMapper().readTree(in);
            List<String> regras = new ArrayList<>();
            node.path("globais").forEach(item -> regras.add(item.asText()));
            if (regras.isEmpty()) {
                throw new InferenceException("Regras globais ausentes");
            }
            return regras;
        } catch (InferenceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InferenceException("Falha ao ler camadas.json: " + ex.getMessage(), ex);
        }
    }

    public PromptLibrary library() {
        return library;
    }

    public FewShotCatalog catalog() {
        return catalog;
    }
}
