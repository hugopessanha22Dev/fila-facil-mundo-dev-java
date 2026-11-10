package com.mundodev.filafacil.pipeline.inter;

import com.mundodev.filafacil.pipeline.io.ParquetIO;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class InterFixtureMaterializer {

    private InterFixtureMaterializer() {
    }

    public static Path ensureInput(Path pipelineRoot) throws Exception {
        Path input = pipelineRoot.resolve("input").resolve("inter");
        Files.createDirectories(input);
        copy("pipeline/fixtures/inter/coletas_api.json", input.resolve("coletas_api.json"));
        copy("pipeline/fixtures/inter/clientes.csv", input.resolve("clientes.csv"));
        copy("pipeline/fixtures/inter/motoristas_rotas.json", input.resolve("motoristas_rotas.json"));
        Path entregasCsv = input.resolve("entregas.csv");
        copy("pipeline/fixtures/inter/entregas.csv", entregasCsv);
        Path parquet = input.resolve("entregas.parquet");
        if (!Files.isRegularFile(parquet) || Files.size(parquet) == 0) {
            ParquetIO.writeEntregasFromCsv(entregasCsv, parquet);
        }
        return input;
    }

    private static void copy(String resource, Path target) throws Exception {
        try (InputStream in = InterFixtureMaterializer.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Fixture ausente: " + resource);
            }
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
