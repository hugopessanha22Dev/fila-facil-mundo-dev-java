package com.mundodev.filafacil.auditoria.model;

import java.time.Instant;
import java.util.Map;

public record AuditEvent(
        String tipo,
        String usuario,
        String entidade,
        String entidadeId,
        Map<String, Object> dadosAntigos,
        Map<String, Object> dadosNovos,
        Instant em) {
}
