package com.mundodev.filafacil.pipeline.stage;

import com.mundodev.filafacil.pipeline.PipelineContext;
import com.mundodev.filafacil.pipeline.PipelineStage;
import com.mundodev.filafacil.pipeline.io.LayerWriter;
import com.mundodev.filafacil.pipeline.model.MetricaDiaria;
import com.mundodev.filafacil.pipeline.store.CadastroRepository;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class LoadStage implements PipelineStage {

    private final CadastroRepository cadastroRepository;

    public LoadStage(CadastroRepository cadastroRepository) {
        this.cadastroRepository = cadastroRepository;
    }

    @Override
    public String name() {
        return "load";
    }

    @Override
    public void run(PipelineContext context) throws Exception {
        Path consolidada = context.paths().consolidada(context.dataRef(), context.runId());
        List<MetricaDiaria> metricas = context.metricas();

        LayerWriter.writeJson(consolidada, "metricas.json", metricas);
        List<String> csv = new ArrayList<>();
        csv.add("data_ref,regiao,status,cliente_codigo,total_coletas,total_entregas,total_atrasos,total_ocorrencias");
        for (MetricaDiaria m : metricas) {
            csv.add(String.join(",",
                    m.dataRef(), m.regiao(), m.status(), m.clienteCodigo(),
                    String.valueOf(m.totalColetas()),
                    String.valueOf(m.totalEntregas()),
                    String.valueOf(m.totalAtrasos()),
                    String.valueOf(m.totalOcorrencias())));
        }
        LayerWriter.writeLines(consolidada, "metricas.csv", csv);

        Optional<String> db = cadastroRepository.loadMetricas(metricas, context.runId());
        context.setMetricasLoadedToDb(db.isPresent());
        context.log().info("carga arquivos=" + consolidada.toAbsolutePath()
                + " db=" + (db.isPresent() ? db.get() : "fallback-arquivo-only"));

        context.log().counters().addGravados(2L + (db.isPresent() ? metricas.size() : 0L));
    }
}
