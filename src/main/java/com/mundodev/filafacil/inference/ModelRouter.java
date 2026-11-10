package com.mundodev.filafacil.inference;

import java.util.Locale;

public class ModelRouter {

    private final InferenceConfig config;

    public ModelRouter(InferenceConfig config) {
        this.config = config;
    }

    public InferenceRequest route(String mensagem) {
        String text = mensagem == null ? "" : mensagem;
        String lower = text.toLowerCase(Locale.ROOT);
        boolean critical = isCritical(lower, text.length());
        ModelProfile profile = critical ? ModelProfile.DETAILED : ModelProfile.LIGHT;
        InferenceParams params = critical ? InferenceParams.forCritical() : InferenceParams.forLight();
        return new InferenceRequest(text, profile, params, config.modelFor(profile));
    }

    private static boolean isCritical(String lower, int length) {
        if (length >= 280) {
            return true;
        }
        return containsAny(lower,
                "reclam", "nao chegou", "não chegou", "urgente", "critico", "crítico",
                "danificado", "perda", "advogad", "procon", "imediato");
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
