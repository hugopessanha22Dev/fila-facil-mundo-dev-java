package com.mundodev.filafacil.pipeline.inter;

import java.time.LocalDateTime;

public record Checkpoint(
        String sourceId,
        LocalDateTime lastTs,
        LocalDateTime intervalFrom,
        LocalDateTime intervalTo,
        String batchId,
        int records,
        String status,
        String runId) {
}
