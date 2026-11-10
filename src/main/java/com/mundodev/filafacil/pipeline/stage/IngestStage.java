package com.mundodev.filafacil.pipeline.stage;

import com.mundodev.filafacil.pipeline.PipelineContext;
import com.mundodev.filafacil.pipeline.PipelineStage;
import com.mundodev.filafacil.pipeline.io.FixtureMaterializer;
import com.mundodev.filafacil.pipeline.io.LayerWriter;
import com.mundodev.filafacil.pipeline.io.SourceReaders;
import com.mundodev.filafacil.pipeline.model.ColetaRaw;
import com.mundodev.filafacil.pipeline.model.EntregaRaw;
import com.mundodev.filafacil.pipeline.model.OcorrenciaRaw;
import com.mundodev.filafacil.pipeline.store.CadastroRepository;

import java.nio.file.Path;
import java.util.List;

public final class IngestStage implements PipelineStage {

    private final CadastroRepository cadastroRepository;

    public IngestStage(CadastroRepository cadastroRepository) {
        this.cadastroRepository = cadastroRepository;
    }

    @Override
    public String name() {
        return "ingest";
    }

    @Override
    public void run(PipelineContext context) throws Exception {
        Path input = context.paths().input();
        FixtureMaterializer.ensureInput(input);

        Path coletasFile = input.resolve("coletas.csv");
        Path ocorrenciasFile = input.resolve("ocorrencias.json");
        Path entregasFile = input.resolve("entregas.parquet");
        Path clientesFallback = input.resolve("clientes_unidades.csv");

        SourceReaders.requireFile(coletasFile, "CSV coletas");
        SourceReaders.requireFile(ocorrenciasFile, "JSON ocorrencias");
        SourceReaders.requireFile(entregasFile, "Parquet entregas");
        SourceReaders.requireFile(clientesFallback, "CSV clientes fallback");

        List<ColetaRaw> coletas = SourceReaders.readColetasCsv(coletasFile);
        List<OcorrenciaRaw> ocorrencias = SourceReaders.readOcorrenciasJson(ocorrenciasFile);
        List<EntregaRaw> entregas = SourceReaders.readEntregasParquet(entregasFile);
        CadastroRepository.CadastroLoad cadastro = cadastroRepository.loadCadastro();

        context.setColetasBrutas(coletas);
        context.setOcorrenciasBrutas(ocorrencias);
        context.setEntregasBrutas(entregas);
        context.setCadastro(cadastro.rows());
        context.setCadastroFromJdbc(cadastro.fromJdbc());

        long lidos = coletas.size() + ocorrencias.size() + entregas.size() + cadastro.rows().size();
        context.log().counters().addLidos(lidos);
        context.log().info("ingest lidos coletas=" + coletas.size()
                + " ocorrencias=" + ocorrencias.size()
                + " entregas=" + entregas.size()
                + " cadastro=" + cadastro.rows().size()
                + " cadastroJdbc=" + cadastro.fromJdbc());

        Path bruta = context.paths().bruta(context.dataRef(), context.runId());
        LayerWriter.writeJson(bruta, "coletas.json", coletas);
        LayerWriter.writeJson(bruta, "ocorrencias.json", ocorrencias);
        LayerWriter.writeJson(bruta, "entregas.json", entregas);
        LayerWriter.writeJson(bruta, "cadastro.json", cadastro.rows().values());
        context.log().counters().addGravados(4);
        context.log().info("camada bruta=" + bruta.toAbsolutePath());
    }
}
