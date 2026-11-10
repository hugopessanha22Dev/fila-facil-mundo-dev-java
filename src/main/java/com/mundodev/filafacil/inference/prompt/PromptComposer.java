package com.mundodev.filafacil.inference.prompt;

public class PromptComposer {

    private final PromptLibrary library;
    private final FewShotCatalog catalog;

    public PromptComposer(PromptLibrary library, FewShotCatalog catalog) {
        this.library = library;
        this.catalog = catalog;
    }

    public String composeSystemPrompt(String taskId, String sessionSummary) {
        PromptDefinition def = library.get(taskId);
        StringBuilder sb = new StringBuilder();
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
        return sb.toString();
    }

    public PromptLibrary library() {
        return library;
    }

    public FewShotCatalog catalog() {
        return catalog;
    }
}
