package com.mundodev.filafacil.pipeline;

public interface PipelineStage {

    String name();

    void run(PipelineContext context) throws Exception;
}
