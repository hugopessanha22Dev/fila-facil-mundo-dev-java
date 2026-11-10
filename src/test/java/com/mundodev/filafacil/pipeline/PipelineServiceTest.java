package com.mundodev.filafacil.pipeline;

import com.mundodev.filafacil.pipeline.io.SourceReaders;
import com.mundodev.filafacil.pipeline.model.ClienteUnidade;
import com.mundodev.filafacil.pipeline.model.ColetaRaw;
import com.mundodev.filafacil.pipeline.model.EntregaRaw;
import com.mundodev.filafacil.pipeline.model.MetricaDiaria;
import com.mundodev.filafacil.pipeline.stage.TransformStage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PipelineServiceTest {

    @TempDir
    Path temp;

    @Test
    void ingestFalhaQuandoFonteAusente() {
        Path missing = temp.resolve("nao-existe.csv");
        assertThrows(IllegalStateException.class,
                () -> SourceReaders.requireFile(missing, "CSV coletas"));
    }

    @Test
    void pipelineCompletoGeraCamadasEMetricas() throws Exception {
        Path root = temp.resolve("pipeline");
        PipelineConfig config = new PipelineConfig(root, "memory", "x", "y", 1);
        PipelineModule.PipelineRunResult result = PipelineModule.run(config, "2026-11-08", "testrun01");

        assertEquals("SUCCESS", result.log().status());
        assertTrue(result.log().counters().lidos() > 0);
        assertTrue(result.log().counters().descartados() >= 1);
        assertFalse(result.context().metricas().isEmpty());

        Path bruta = result.context().paths().bruta("2026-11-08", "testrun01");
        Path processada = result.context().paths().processada("2026-11-08", "testrun01");
        Path consolidada = result.context().paths().consolidada("2026-11-08", "testrun01");
        assertTrue(Files.isRegularFile(bruta.resolve("coletas.json")));
        assertTrue(Files.isRegularFile(processada.resolve("metricas.json")));
        assertTrue(Files.isRegularFile(consolidada.resolve("metricas.csv")));
        assertTrue(Files.isRegularFile(root.resolve("input").resolve("entregas.parquet")));
        assertTrue(Files.isRegularFile(result.context().paths().logFile("testrun01")));
        assertFalse(result.context().metricasLoadedToDb());
    }

    @Test
    void transformDescartaIncompletoEEnriqueceComCadastro() {
        ColetaRaw invalida = new ColetaRaw("", "CLI-001", "sudeste", "Ana", "concluida", "", "", "2026-11-08");
        ColetaRaw valida = new ColetaRaw("COL-1", "CLI-001", "sudeste", "Ana", "concluida", "2026-11-08", "2026-11-08", "2026-11-08");
        EntregaRaw entrega = new EntregaRaw("ENT-1", "sudeste", "2026-11-08", "2026-11-08", "concluida", false, "CLI-001", "");
        Map<String, ClienteUnidade> cadastro = Map.of(
                "CLI-001", new ClienteUnidade("CLI-001", "Mercado Norte", "UN-SP-01", "SUDESTE"));

        ColetaRaw nInvalida = TransformStage.normalizeColeta(invalida);
        assertTrue(nInvalida.codigoColeta().isBlank());

        EntregaRaw enriquecida = TransformStage.normalizeEntrega(entrega, cadastro);
        assertEquals("SUDESTE", enriquecida.regiao());
        assertEquals("UN-SP-01", enriquecida.unidade());

        List<MetricaDiaria> metricas = TransformStage.aggregate(
                List.of(TransformStage.normalizeColeta(valida)),
                List.of(enriquecida),
                List.of());
        assertFalse(metricas.isEmpty());
        assertTrue(metricas.stream().anyMatch(m -> m.totalColetas() >= 1 || m.totalEntregas() >= 1));
    }

    @Test
    void orquestradorBloqueiaEtapasAposFalha() {
        AtomicBoolean transformCalled = new AtomicBoolean(false);
        PipelineRunLog log = new PipelineRunLog("fail01");
        PipelineConfig config = new PipelineConfig(temp, "memory", "x", "y", 0);
        PipelinePaths paths = new PipelinePaths(temp);
        PipelineContext ctx = new PipelineContext(config, paths, "fail01", "2026-11-08", log);

        PipelineStage ingestFail = new PipelineStage() {
            @Override
            public String name() {
                return "ingest";
            }

            @Override
            public void run(PipelineContext context) {
                throw new IllegalStateException("fonte ausente");
            }
        };
        PipelineStage transform = new PipelineStage() {
            @Override
            public String name() {
                return "transform";
            }

            @Override
            public void run(PipelineContext context) {
                transformCalled.set(true);
            }
        };

        PipelineRunLog result = new PipelineOrchestrator(List.of(ingestFail, transform), 0).run(ctx);
        assertEquals("FAILED", result.status());
        assertTrue(result.erro().contains("ingest"));
        assertFalse(transformCalled.get());
    }
}
