package com.mundodev.filafacil.pipeline;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public final class PipelineOrchestrator {

    private final List<PipelineStage> stages;
    private final int maxRetries;

    public PipelineOrchestrator(List<PipelineStage> stages, int maxRetries) {
        this.stages = List.copyOf(stages);
        this.maxRetries = Math.max(0, maxRetries);
    }

    public PipelineRunLog run(PipelineContext context) {
        PipelineRunLog log = context.log();
        for (PipelineStage stage : stages) {
            log.etapaInicio(stage.name());
            long started = System.currentTimeMillis();
            try {
                runWithRetry(stage, context);
                log.etapaFim(stage.name(), System.currentTimeMillis() - started);
            } catch (Exception ex) {
                log.etapaFim(stage.name(), System.currentTimeMillis() - started);
                log.falha(stage.name(), ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
                persistLog(context);
                return log;
            }
        }
        log.sucesso();
        persistLog(context);
        return log;
    }

    private void runWithRetry(PipelineStage stage, PipelineContext context) throws Exception {
        int attempts = 0;
        Exception last = null;
        while (attempts <= maxRetries) {
            try {
                stage.run(context);
                return;
            } catch (Exception ex) {
                last = ex;
                attempts++;
                context.log().info("retry etapa=" + stage.name() + " tentativa=" + attempts + " erro=" + ex.getMessage());
                if (attempts > maxRetries) {
                    break;
                }
                Thread.sleep(50L * attempts);
            }
        }
        throw last == null ? new IllegalStateException("falha desconhecida em " + stage.name()) : last;
    }

    private void persistLog(PipelineContext context) {
        try {
            Files.createDirectories(context.paths().logs());
            Files.writeString(context.paths().logFile(context.runId()), context.log().asText(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            // log em arquivo e best-effort
        }
    }
}
