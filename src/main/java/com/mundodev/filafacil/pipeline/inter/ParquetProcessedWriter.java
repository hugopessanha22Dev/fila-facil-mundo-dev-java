package com.mundodev.filafacil.pipeline.inter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class ParquetProcessedWriter {

    private ParquetProcessedWriter() {
    }

    public static Path write(Path processadaRoot, String runId, List<SourceRecord> records) throws Exception {
        Files.createDirectories(processadaRoot);
        Path csv = processadaRoot.resolve("_staging-" + runId + ".csv");
        List<String> lines = new ArrayList<>();
        lines.add("business_key,entity,source,regiao,dt,updated_at,version,deleted,cdc_op,late_arriving");
        for (SourceRecord r : records) {
            lines.add(String.join(",",
                    csv(r.businessKey()),
                    csv(r.entity()),
                    csv(r.source().id()),
                    csv(r.regiao()),
                    csv(r.dt()),
                    csv(r.updatedAt().toString()),
                    String.valueOf(r.version()),
                    String.valueOf(r.deleted()),
                    csv(r.cdcOp().name()),
                    String.valueOf(r.lateArriving())));
        }
        Files.write(csv, lines, StandardCharsets.UTF_8);

        Path outDir = processadaRoot.resolve("run=" + runId);
        Files.createDirectories(outDir);
        String csvPath = csv.toAbsolutePath().toString().replace('\\', '/');
        String outPath = outDir.toAbsolutePath().toString().replace('\\', '/');

        try (Connection c = DriverManager.getConnection("jdbc:duckdb:");
             Statement st = c.createStatement()) {
            st.execute("CREATE OR REPLACE TABLE processed AS SELECT * FROM read_csv_auto('" + csvPath + "', HEADER=true)");
            // DuckDB partition write
            st.execute("COPY processed TO '" + outPath + "' (FORMAT PARQUET, PARTITION_BY (dt, regiao), OVERWRITE_OR_IGNORE)");
        }
        return outDir;
    }

    private static String csv(String v) {
        if (v == null) {
            return "";
        }
        if (v.contains(",") || v.contains("\"")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
