package com.mundodev.filafacil.aidevops;

public record ModelSettings(
        String env,
        String primaryModel,
        String fallbackModel,
        int maxRetries,
        int rateLimitPerMinute) {
}
