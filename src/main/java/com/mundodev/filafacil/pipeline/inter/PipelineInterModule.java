package com.mundodev.filafacil.pipeline.inter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class PipelineInterModule {

    private PipelineInterModule() {
    }

    public record InterRunResult(InterRunReport report, Path consolidadaState, Path lineageFile, Path processadaDir) {
    }

    public static InterRunResult run(InterConfig config) throws Exception {
        String runId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        InterRunReport report = new InterRunReport(runId, config.mode());
        Path root = config.pipelineRoot();
        Path input = InterFixtureMaterializer.ensureInput(root);
        Path checkpointsDir = root.resolve("checkpoints");
        Path consolidadaDir = root.resolve("consolidada").resolve("inter");
        Path processadaDir = root.resolve("processada").resolve("inter");
        Path lineageDir = root.resolve("lineage");
        Path logsDir = root.resolve("logs");
        Files.createDirectories(logsDir);

        CheckpointStore checkpoints = new CheckpointStore(checkpointsDir);
        ConsolidatedStore consolidatedStore = new ConsolidatedStore(consolidadaDir);
        LineageStore lineage = new LineageStore(lineageDir, runId);
        Map<String, SourceRecord> previous = consolidatedStore.load();
        MergeEngine merge = new MergeEngine(previous);

        Set<SourceId> sources = config.onlySource().isPresent()
                ? EnumSet.of(config.onlySource().get())
                : EnumSet.allOf(SourceId.class);

        List<SourceRecord> collected = new ArrayList<>();
        ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, config.parallelism()));
        try {
            List<Future<SourceBatch>> futures = new ArrayList<>();
            for (SourceId source : sources) {
                futures.add(pool.submit(() -> readSourceBatch(config, checkpoints, input, source, report)));
            }
            for (Future<SourceBatch> f : futures) {
                SourceBatch batch = f.get();
                if (batch != null) {
                    collected.addAll(batch.records());
                }
            }
        } catch (Exception ex) {
            report.falha("ingest", ex.getMessage());
            persistLog(logsDir, report);
            throw ex;
        } finally {
            pool.shutdownNow();
        }

        try {
            // watermark: min checkpoint among selected sources (or reprocess from)
            LocalDateTime watermark = resolveWatermark(config, checkpoints, sources);
            LateArrivingHandler lateHandler = new LateArrivingHandler(config.latePolicy());
            LateArrivingHandler.LateResult lateResult = lateHandler.apply(collected, watermark);
            long discarded = lateResult.rejected();

            // classify against previous before merge for lineage
            for (SourceRecord r : lateResult.accepted()) {
                r.setCdcOp(CdcClassifier.classify(r, merge.state()));
            }

            merge.applyAll(lateResult.accepted());
            MergeEngine.MergeStats stats = merge.stats();

            for (SourceRecord r : merge.values()) {
                lineage.addWithRun(runId, r, "normalize,dedup,cdc,merge", consolidadaDir.resolve("state.json").toString());
            }

            Path parquetOut = ParquetProcessedWriter.write(processadaDir, runId, merge.values());
            consolidatedStore.save(merge.values());
            lineage.flush();

            // update checkpoints only on success, per source
            for (SourceId source : sources) {
                List<SourceRecord> ofSource = lateResult.accepted().stream()
                        .filter(r -> r.source() == source)
                        .toList();
                LocalDateTime maxTs = ofSource.stream()
                        .map(SourceRecord::updatedAt)
                        .max(LocalDateTime::compareTo)
                        .orElse(checkpoints.lastTsOrMin(source.id()));
                LocalDateTime from = ofSource.stream()
                        .map(SourceRecord::updatedAt)
                        .min(LocalDateTime::compareTo)
                        .orElse(maxTs);
                checkpoints.save(new Checkpoint(
                        source.id(),
                        maxTs,
                        from,
                        maxTs,
                        runId,
                        ofSource.size(),
                        "SUCCESS",
                        runId));
                report.info("checkpoint source=" + source.id() + " last_ts=" + maxTs + " records=" + ofSource.size());
            }

            report.sucesso(stats, lateResult.lateCount(), lateResult.rejected(), discarded);
            report.info("processada=" + parquetOut.toAbsolutePath());
            report.info("consolidada=" + consolidatedStore.file().toAbsolutePath());
            report.info("lineage=" + lineage.file().toAbsolutePath());
            persistLog(logsDir, report);
            return new InterRunResult(report, consolidatedStore.file(), lineage.file(), parquetOut);
        } catch (Exception ex) {
            report.falha("transform/load", ex.getMessage());
            persistLog(logsDir, report);
            throw ex;
        }
    }

    private static SourceBatch readSourceBatch(
            InterConfig config,
            CheckpointStore checkpoints,
            Path input,
            SourceId source,
            InterRunReport report) throws Exception {
        long started = System.currentTimeMillis();
        List<SourceRecord> raw = SourceAdapters.readSource(source, input);
        List<SourceRecord> filtered;
        if (config.mode() == InterMode.BATCH && !config.reprocess()) {
            filtered = raw;
        } else if (config.reprocess()) {
            LocalDateTime from = config.reprocessFrom().orElse(LocalDateTime.MIN);
            LocalDateTime to = config.reprocessTo().orElse(LocalDateTime.MAX);
            filtered = raw.stream()
                    .filter(r -> !r.updatedAt().isBefore(from) && !r.updatedAt().isAfter(to))
                    .toList();
        } else {
            LocalDateTime after = checkpoints.lastTsOrMin(source.id());
            filtered = SourceAdapters.filterIncremental(raw, after);
        }
        long ms = System.currentTimeMillis() - started;
        synchronized (report) {
            report.sourceVolume(source.id(), filtered.size(), ms);
        }
        return new SourceBatch(source, filtered);
    }

    private static LocalDateTime resolveWatermark(
            InterConfig config,
            CheckpointStore checkpoints,
            Set<SourceId> sources) throws Exception {
        if (config.reprocessFrom().isPresent()) {
            return config.reprocessFrom().get();
        }
        LocalDateTime min = null;
        for (SourceId s : sources) {
            LocalDateTime ts = checkpoints.lastTsOrMin(s.id());
            if (ts.equals(LocalDateTime.MIN)) {
                continue;
            }
            if (min == null || ts.isBefore(min)) {
                min = ts;
            }
        }
        // first run: no watermark (accept all); subsequent: earliest checkpoint
        return min == null ? LocalDateTime.MIN : min.minusHours(1);
    }

    private static void persistLog(Path logsDir, InterRunReport report) {
        try {
            Files.createDirectories(logsDir);
            Files.writeString(logsDir.resolve("inter-run-" + report.runId() + ".log"),
                    report.asText(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    private record SourceBatch(SourceId source, List<SourceRecord> records) {
    }
}
