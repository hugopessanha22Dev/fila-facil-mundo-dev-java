package com.mundodev.filafacil.pipeline;

import java.nio.file.Path;

public final class PipelinePaths {

    private final Path root;

    public PipelinePaths(Path root) {
        this.root = root;
    }

    public Path root() {
        return root;
    }

    public Path input() {
        return root.resolve("input");
    }

    public Path bruta(String dt, String runId) {
        return root.resolve("bruta").resolve("dt=" + dt).resolve("run=" + runId);
    }

    public Path processada(String dt, String runId) {
        return root.resolve("processada").resolve("dt=" + dt).resolve("run=" + runId);
    }

    public Path consolidada(String dt, String runId) {
        return root.resolve("consolidada").resolve("dt=" + dt).resolve("run=" + runId);
    }

    public Path logs() {
        return root.resolve("logs");
    }

    public Path logFile(String runId) {
        return logs().resolve("run-" + runId + ".log");
    }
}
