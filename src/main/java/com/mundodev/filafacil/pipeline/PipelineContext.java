package com.mundodev.filafacil.pipeline;

import com.mundodev.filafacil.pipeline.model.ClienteUnidade;
import com.mundodev.filafacil.pipeline.model.ColetaRaw;
import com.mundodev.filafacil.pipeline.model.EntregaRaw;
import com.mundodev.filafacil.pipeline.model.MetricaDiaria;
import com.mundodev.filafacil.pipeline.model.OcorrenciaRaw;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PipelineContext {

    private final PipelineConfig config;
    private final PipelinePaths paths;
    private final String runId;
    private final String dataRef;
    private final PipelineRunLog log;

    private List<ColetaRaw> coletasBrutas = List.of();
    private List<OcorrenciaRaw> ocorrenciasBrutas = List.of();
    private List<EntregaRaw> entregasBrutas = List.of();
    private Map<String, ClienteUnidade> cadastro = Map.of();

    private List<ColetaRaw> coletasProcessadas = new ArrayList<>();
    private List<OcorrenciaRaw> ocorrenciasProcessadas = new ArrayList<>();
    private List<EntregaRaw> entregasProcessadas = new ArrayList<>();
    private List<MetricaDiaria> metricas = new ArrayList<>();

    private boolean cadastroFromJdbc;
    private boolean metricasLoadedToDb;

    public PipelineContext(PipelineConfig config, PipelinePaths paths, String runId, String dataRef, PipelineRunLog log) {
        this.config = config;
        this.paths = paths;
        this.runId = runId;
        this.dataRef = dataRef;
        this.log = log;
    }

    public PipelineConfig config() {
        return config;
    }

    public PipelinePaths paths() {
        return paths;
    }

    public String runId() {
        return runId;
    }

    public String dataRef() {
        return dataRef;
    }

    public PipelineRunLog log() {
        return log;
    }

    public List<ColetaRaw> coletasBrutas() {
        return coletasBrutas;
    }

    public void setColetasBrutas(List<ColetaRaw> coletasBrutas) {
        this.coletasBrutas = List.copyOf(coletasBrutas);
    }

    public List<OcorrenciaRaw> ocorrenciasBrutas() {
        return ocorrenciasBrutas;
    }

    public void setOcorrenciasBrutas(List<OcorrenciaRaw> ocorrenciasBrutas) {
        this.ocorrenciasBrutas = List.copyOf(ocorrenciasBrutas);
    }

    public List<EntregaRaw> entregasBrutas() {
        return entregasBrutas;
    }

    public void setEntregasBrutas(List<EntregaRaw> entregasBrutas) {
        this.entregasBrutas = List.copyOf(entregasBrutas);
    }

    public Map<String, ClienteUnidade> cadastro() {
        return cadastro;
    }

    public void setCadastro(Map<String, ClienteUnidade> cadastro) {
        this.cadastro = Map.copyOf(cadastro);
    }

    public List<ColetaRaw> coletasProcessadas() {
        return coletasProcessadas;
    }

    public List<OcorrenciaRaw> ocorrenciasProcessadas() {
        return ocorrenciasProcessadas;
    }

    public List<EntregaRaw> entregasProcessadas() {
        return entregasProcessadas;
    }

    public List<MetricaDiaria> metricas() {
        return metricas;
    }

    public void setMetricas(List<MetricaDiaria> metricas) {
        this.metricas = new ArrayList<>(metricas);
    }

    public boolean cadastroFromJdbc() {
        return cadastroFromJdbc;
    }

    public void setCadastroFromJdbc(boolean cadastroFromJdbc) {
        this.cadastroFromJdbc = cadastroFromJdbc;
    }

    public boolean metricasLoadedToDb() {
        return metricasLoadedToDb;
    }

    public void setMetricasLoadedToDb(boolean metricasLoadedToDb) {
        this.metricasLoadedToDb = metricasLoadedToDb;
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> m = new HashMap<>();
        m.put("runId", runId);
        m.put("dataRef", dataRef);
        m.put("status", log.status());
        m.put("lidos", log.counters().lidos());
        m.put("descartados", log.counters().descartados());
        m.put("gravados", log.counters().gravados());
        m.put("cadastroFromJdbc", cadastroFromJdbc);
        m.put("metricasLoadedToDb", metricasLoadedToDb);
        m.put("metricas", metricas.size());
        return m;
    }
}
