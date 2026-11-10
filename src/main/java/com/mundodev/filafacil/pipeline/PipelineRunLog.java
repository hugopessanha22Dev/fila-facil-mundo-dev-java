package com.mundodev.filafacil.pipeline;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PipelineRunLog {

    private final String runId;
    private final Instant inicio;
    private Instant fim;
    private String status = "RUNNING";
    private String erro;
    private final Map<String, Long> tempoMsPorEtapa = new LinkedHashMap<>();
    private final List<String> linhas = new ArrayList<>();
    private final PipelineCounters counters = new PipelineCounters();

    public PipelineRunLog(String runId) {
        this.runId = runId;
        this.inicio = Instant.now();
        linhas.add("runId=" + runId + " inicio=" + inicio);
    }

    public void etapaInicio(String etapa) {
        linhas.add("ETAPA_INICIO " + etapa + " em " + Instant.now());
    }

    public void etapaFim(String etapa, long ms) {
        tempoMsPorEtapa.put(etapa, ms);
        linhas.add("ETAPA_FIM " + etapa + " ms=" + ms);
    }

    public void info(String msg) {
        linhas.add(msg);
    }

    public void sucesso() {
        this.fim = Instant.now();
        this.status = "SUCCESS";
        linhas.add("status=SUCCESS fim=" + fim
                + " lidos=" + counters.lidos()
                + " descartados=" + counters.descartados()
                + " gravados=" + counters.gravados());
    }

    public void falha(String etapa, String motivo) {
        this.fim = Instant.now();
        this.status = "FAILED";
        this.erro = etapa + ": " + motivo;
        linhas.add("status=FAILED etapa=" + etapa + " motivo=" + motivo + " fim=" + fim);
    }

    public String runId() {
        return runId;
    }

    public Instant inicio() {
        return inicio;
    }

    public Instant fim() {
        return fim;
    }

    public String status() {
        return status;
    }

    public String erro() {
        return erro;
    }

    public Map<String, Long> tempoMsPorEtapa() {
        return Collections.unmodifiableMap(tempoMsPorEtapa);
    }

    public PipelineCounters counters() {
        return counters;
    }

    public List<String> linhas() {
        return Collections.unmodifiableList(linhas);
    }

    public String resumo() {
        return "status=" + status
                + " lidos=" + counters.lidos()
                + " descartados=" + counters.descartados()
                + " gravados=" + counters.gravados()
                + (erro != null ? " erro=" + erro : "");
    }

    public String asText() {
        return String.join(System.lineSeparator(), linhas);
    }
}
