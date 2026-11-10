package com.mundodev.filafacil.pipeline.inter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class InterRunReport {

    private final String runId;
    private final InterMode mode;
    private final List<String> lines = new ArrayList<>();
    private final Map<String, Long> lidosPorFonte = new LinkedHashMap<>();
    private final Map<String, Long> tempoMsPorFonte = new LinkedHashMap<>();
    private final List<String> alerts = new ArrayList<>();
    private String status = "RUNNING";
    private String error;
    private MergeEngine.MergeStats mergeStats;
    private long lateCount;
    private long rejectedLate;
    private long discarded;

    public InterRunReport(String runId, InterMode mode) {
        this.runId = runId;
        this.mode = mode;
        lines.add("runId=" + runId + " mode=" + mode);
    }

    public void info(String msg) {
        lines.add(msg);
    }

    public void alert(String msg) {
        String a = "ALERT " + msg;
        alerts.add(a);
        lines.add(a);
        System.err.println(a);
    }

    public void sourceVolume(String sourceId, long lidos, long ms) {
        lidosPorFonte.put(sourceId, lidos);
        tempoMsPorFonte.put(sourceId, ms);
        lines.add("fonte=" + sourceId + " lidos=" + lidos + " ms=" + ms);
        if (lidos == 0) {
            alert("volume inesperado: fonte " + sourceId + " com 0 registros lidos");
        }
    }

    public void sucesso(MergeEngine.MergeStats stats, long late, long rejected, long discarded) {
        this.mergeStats = stats;
        this.lateCount = late;
        this.rejectedLate = rejected;
        this.discarded = discarded;
        this.status = "SUCCESS";
        lines.add(resumo());
    }

    public void falha(String etapa, String motivo) {
        this.status = "FAILED";
        this.error = etapa + ": " + motivo;
        alert("falha etapa=" + etapa + " motivo=" + motivo);
        lines.add("status=FAILED " + error);
    }

    public String resumo() {
        MergeEngine.MergeStats s = mergeStats == null
                ? new MergeEngine.MergeStats(0, 0, 0, 0, 0)
                : mergeStats;
        return "status=" + status
                + " mode=" + mode
                + " inseridos=" + s.inserted()
                + " atualizados=" + s.updated()
                + " deletados=" + s.deleted()
                + " deduplicados=" + s.deduplicated()
                + " stale=" + s.skippedStale()
                + " late=" + lateCount
                + " rejectedLate=" + rejectedLate
                + " descartados=" + discarded
                + (error != null ? " erro=" + error : "");
    }

    public String runId() {
        return runId;
    }

    public InterMode mode() {
        return mode;
    }

    public String status() {
        return status;
    }

    public String error() {
        return error;
    }

    public Map<String, Long> lidosPorFonte() {
        return lidosPorFonte;
    }

    public Map<String, Long> tempoMsPorFonte() {
        return tempoMsPorFonte;
    }

    public List<String> alerts() {
        return List.copyOf(alerts);
    }

    public List<String> lines() {
        return List.copyOf(lines);
    }

    public MergeEngine.MergeStats mergeStats() {
        return mergeStats;
    }

    public String asText() {
        return String.join(System.lineSeparator(), lines);
    }
}
