package com.mundodev.filafacil.inference;

public record InferenceRequest(
        String mensagem,
        ModelProfile profile,
        InferenceParams params,
        String modelId,
        String systemPrompt) {

    public InferenceRequest(String mensagem, ModelProfile profile, InferenceParams params, String modelId) {
        this(mensagem, profile, params, modelId, null);
    }

    public String resolvedSystemPrompt() {
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            return systemPrompt;
        }
        return DefaultPrompts.TRIAGE_ONE_SHOT;
    }
}
