package com.mundodev.filafacil.inference;

public record InferenceConfig(
        String mode,
        String apiKey,
        String baseUrl,
        String modelLight,
        String modelDetailed,
        int timeoutSeconds,
        int maxRetries,
        double pricePer1kTokensUsd) {

    public static InferenceConfig fromEnvironment() {
        String mode = env("FILAFACIL_LLM_MODE", "mock").trim().toLowerCase();
        return new InferenceConfig(
                mode,
                env("FILAFACIL_LLM_API_KEY", ""),
                env("FILAFACIL_LLM_BASE_URL", "https://api.openai.com/v1"),
                env("FILAFACIL_LLM_MODEL_LIGHT", "gpt-4o-mini"),
                env("FILAFACIL_LLM_MODEL_DETAILED", "gpt-4o"),
                Integer.parseInt(env("FILAFACIL_LLM_TIMEOUT_SECONDS", "20")),
                Integer.parseInt(env("FILAFACIL_LLM_MAX_RETRIES", "2")),
                Double.parseDouble(env("FILAFACIL_LLM_PRICE_PER_1K", "0.00015")));
    }

    public boolean isMock() {
        return !"http".equalsIgnoreCase(mode);
    }

    public String modelFor(ModelProfile profile) {
        return profile == ModelProfile.DETAILED ? modelDetailed : modelLight;
    }

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
