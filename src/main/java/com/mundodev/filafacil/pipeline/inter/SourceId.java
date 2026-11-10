package com.mundodev.filafacil.pipeline.inter;

public enum SourceId {
    API_COLETAS("api_coletas", 30),
    CLIENTES("clientes", 40),
    ENTREGAS("entregas", 20),
    MOTORISTAS("motoristas", 10);

    private final String id;
    private final int priority;

    SourceId(String id, int priority) {
        this.id = id;
        this.priority = priority;
    }

    public String id() {
        return id;
    }

    public int priority() {
        return priority;
    }
}
