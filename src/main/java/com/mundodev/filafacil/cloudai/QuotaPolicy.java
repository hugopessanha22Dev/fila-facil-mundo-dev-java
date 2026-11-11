package com.mundodev.filafacil.cloudai;

public record QuotaPolicy(
        int timeoutSeconds,
        int maxTokens,
        int rateLimitPerMinute,
        int tokenQuota,
        int alertTokenThreshold,
        int cacheTtlSeconds,
        int maxAttempts) {
}
