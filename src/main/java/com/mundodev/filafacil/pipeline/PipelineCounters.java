package com.mundodev.filafacil.pipeline;

public final class PipelineCounters {

    private long lidos;
    private long descartados;
    private long gravados;

    public void addLidos(long n) {
        lidos += n;
    }

    public void addDescartados(long n) {
        descartados += n;
    }

    public void addGravados(long n) {
        gravados += n;
    }

    public long lidos() {
        return lidos;
    }

    public long descartados() {
        return descartados;
    }

    public long gravados() {
        return gravados;
    }
}
