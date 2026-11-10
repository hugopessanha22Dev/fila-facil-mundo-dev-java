package com.mundodev.filafacil.pipeline.inter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PipelineInterTest {

    @TempDir
    Path temp;

    @Test
    void mergePrefereVersaoMaisNovaENaoSobrescreveComStale() {
        SourceRecord old = record(SourceId.API_COLETAS, "coleta", "COL-1", "2026-11-08T10:00:00", 1, false);
        SourceRecord newer = record(SourceId.API_COLETAS, "coleta", "COL-1", "2026-11-09T10:00:00", 2, false);
        SourceRecord stale = record(SourceId.ENTREGAS, "coleta", "COL-1", "2026-11-07T10:00:00", 0, false);

        MergeEngine merge = new MergeEngine();
        merge.apply(old);
        merge.apply(newer);
        merge.apply(stale);

        assertEquals(1, merge.state().size());
        assertEquals(2, merge.state().values().iterator().next().version());
        assertEquals(1, merge.stats().inserted());
        assertEquals(1, merge.stats().updated());
        assertTrue(merge.stats().skippedStale() >= 1);
    }

    @Test
    void lateArrivingRejectDescarta() {
        LateArrivingHandler handler = new LateArrivingHandler(LatePolicy.REJECT);
        SourceRecord late = record(SourceId.MOTORISTAS, "motorista", "MOT-1", "2026-11-01T00:00:00", 1, false);
        SourceRecord ok = record(SourceId.MOTORISTAS, "motorista", "MOT-2", "2026-11-10T00:00:00", 1, false);
        LateArrivingHandler.LateResult result = handler.apply(
                List.of(late, ok), LocalDateTime.parse("2026-11-08T00:00:00"));
        assertEquals(1, result.accepted().size());
        assertEquals(1, result.lateCount());
        assertEquals(1, result.rejected());
    }

    @Test
    void checkpointNaoAvancaQuandoNaoSalvoAposFalhaSimulada() throws Exception {
        CheckpointStore store = new CheckpointStore(temp.resolve("checkpoints"));
        store.save(new Checkpoint("api_coletas", LocalDateTime.parse("2026-11-08T10:00:00"),
                LocalDateTime.parse("2026-11-08T10:00:00"), LocalDateTime.parse("2026-11-08T10:00:00"),
                "b1", 3, "SUCCESS", "r1"));
        assertEquals(LocalDateTime.parse("2026-11-08T10:00:00"), store.lastTsOrMin("api_coletas"));
        // falha: nao chama save → checkpoint permanece
        assertEquals(LocalDateTime.parse("2026-11-08T10:00:00"), store.lastTsOrMin("api_coletas"));
    }

    @Test
    void incrementalLeSoDeltaAposBatch() throws Exception {
        Path root = temp.resolve("pipe");
        InterConfig batch = new InterConfig(root, InterMode.BATCH, LatePolicy.MARK, 2,
                Optional.empty(), Optional.empty(), Optional.empty(), false);
        PipelineInterModule.InterRunResult first = PipelineInterModule.run(batch);
        assertEquals("SUCCESS", first.report().status());
        long firstTotal = first.report().lidosPorFonte().values().stream().mapToLong(Long::longValue).sum();
        assertTrue(firstTotal > 0);
        assertTrue(Files.isDirectory(first.processadaDir()));
        assertTrue(Files.isRegularFile(first.lineageFile()));
        assertTrue(Files.isRegularFile(first.consolidadaState()));

        InterConfig incremental = new InterConfig(root, InterMode.INCREMENTAL, LatePolicy.MARK, 2,
                Optional.empty(), Optional.empty(), Optional.empty(), false);
        PipelineInterModule.InterRunResult second = PipelineInterModule.run(incremental);
        assertEquals("SUCCESS", second.report().status());
        long secondTotal = second.report().lidosPorFonte().values().stream().mapToLong(Long::longValue).sum();
        assertTrue(secondTotal < firstTotal, "incremental deve ler menos que batch completo");
    }

    @Test
    void reprocessamentoIdempotenteNaoDuplicaChaves() throws Exception {
        Path root = temp.resolve("pipe2");
        InterConfig batch = new InterConfig(root, InterMode.BATCH, LatePolicy.ACCEPT, 2,
                Optional.empty(), Optional.empty(), Optional.empty(), false);
        PipelineInterModule.run(batch);

        InterConfig reprocess = new InterConfig(root, InterMode.BATCH, LatePolicy.ACCEPT, 2,
                Optional.empty(),
                Optional.of(LocalDateTime.parse("2026-11-01T00:00:00")),
                Optional.of(LocalDateTime.parse("2026-11-30T23:59:59")),
                true);
        PipelineInterModule.InterRunResult again = PipelineInterModule.run(reprocess);
        assertEquals("SUCCESS", again.report().status());

        ConsolidatedStore store = new ConsolidatedStore(root.resolve("consolidada").resolve("inter"));
        Map<String, SourceRecord> state = store.load();
        long col100 = state.keySet().stream().filter(k -> k.endsWith("|COL-100")).count();
        assertEquals(1, col100);
    }

    @Test
    void cdcDeleteLogico() {
        MergeEngine merge = new MergeEngine();
        merge.apply(record(SourceId.ENTREGAS, "entrega", "ENT-1", "2026-11-09T10:00:00", 1, false));
        merge.apply(record(SourceId.ENTREGAS, "entrega", "ENT-1", "2026-11-10T10:00:00", 2, true));
        SourceRecord finalRec = merge.state().get("entrega|ENT-1");
        assertTrue(finalRec.deleted());
        assertEquals(CdcOp.DELETE, finalRec.cdcOp());
    }

    private static SourceRecord record(
            SourceId source, String entity, String key, String ts, int version, boolean deleted) {
        return new SourceRecord(
                source, entity, key, LocalDateTime.parse(ts), version, deleted,
                "SUDESTE", ts.substring(0, 10), Map.of("k", key));
    }
}
