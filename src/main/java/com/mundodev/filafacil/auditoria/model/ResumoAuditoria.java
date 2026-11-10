package com.mundodev.filafacil.auditoria.model;

public record ResumoAuditoria(
        long totalEntregas,
        long totalReembolsosPendentes,
        long totalReembolsosAprovados,
        boolean fromCache) {
}
