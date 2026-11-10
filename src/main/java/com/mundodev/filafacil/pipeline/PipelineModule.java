package com.mundodev.filafacil.pipeline;

import com.mundodev.filafacil.pipeline.stage.IngestStage;
import com.mundodev.filafacil.pipeline.stage.LoadStage;
import com.mundodev.filafacil.pipeline.stage.TransformStage;
import com.mundodev.filafacil.pipeline.store.CadastroRepository;

import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class PipelineModule {

    private PipelineModule() {
    }

    public static PipelineRunResult run(PipelineConfig config) throws Exception {
        return run(config, LocalDate.now().toString(), shortRunId());
    }

    public static PipelineRunResult run(PipelineConfig config, String dataRef, String runId) throws Exception {
        PipelinePaths paths = new PipelinePaths(config.pipelineRoot());
        Files.createDirectories(paths.input());
        Files.createDirectories(paths.logs());

        PipelineRunLog log = new PipelineRunLog(runId);
        PipelineContext context = new PipelineContext(config, paths, runId, dataRef, log);

        try (CadastroRepository repo = CadastroRepository.open(config, paths.input().resolve("clientes_unidades.csv"))) {
            PipelineOrchestrator orchestrator = new PipelineOrchestrator(
                    List.of(
                            new IngestStage(repo),
                            new TransformStage(),
                            new LoadStage(repo)),
                    config.maxRetries());
            PipelineRunLog result = orchestrator.run(context);
            return new PipelineRunResult(result, context);
        }
    }

    private static String shortRunId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    public record PipelineRunResult(PipelineRunLog log, PipelineContext context) {
    }
}
