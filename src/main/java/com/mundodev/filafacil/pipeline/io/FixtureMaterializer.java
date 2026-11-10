package com.mundodev.filafacil.pipeline.io;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Copia fixtures classpath para data/pipeline/input e gera entregas.parquet.
 */
public final class FixtureMaterializer {

    private FixtureMaterializer() {
    }

    public static void ensureInput(Path inputDir) throws Exception {
        Files.createDirectories(inputDir);
        copyResource("pipeline/fixtures/coletas.csv", inputDir.resolve("coletas.csv"));
        copyResource("pipeline/fixtures/ocorrencias.json", inputDir.resolve("ocorrencias.json"));
        copyResource("pipeline/fixtures/clientes_unidades.csv", inputDir.resolve("clientes_unidades.csv"));
        Path entregasCsv = inputDir.resolve("entregas.csv");
        copyResource("pipeline/fixtures/entregas.csv", entregasCsv);
        Path parquet = inputDir.resolve("entregas.parquet");
        if (!Files.isRegularFile(parquet) || Files.size(parquet) == 0) {
            ParquetIO.writeEntregasFromCsv(entregasCsv, parquet);
        }
    }

    private static void copyResource(String resource, Path target) throws Exception {
        try (InputStream in = FixtureMaterializer.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Fixture classpath ausente: " + resource);
            }
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
