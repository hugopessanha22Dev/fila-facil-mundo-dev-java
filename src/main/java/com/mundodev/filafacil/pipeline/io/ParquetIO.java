package com.mundodev.filafacil.pipeline.io;

import com.mundodev.filafacil.pipeline.model.EntregaRaw;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Leitura/escrita Parquet via DuckDB JDBC (formato Parquet real, footprint menor que Hadoop).
 */
public final class ParquetIO {

    private ParquetIO() {
    }

    public static void writeEntregasFromCsv(Path csvSource, Path parquetTarget) throws Exception {
        Files.createDirectories(parquetTarget.getParent());
        String csv = csvSource.toAbsolutePath().toString().replace('\\', '/');
        String out = parquetTarget.toAbsolutePath().toString().replace('\\', '/');
        try (Connection c = DriverManager.getConnection("jdbc:duckdb:");
             Statement st = c.createStatement()) {
            st.execute("CREATE OR REPLACE TABLE entregas AS SELECT * FROM read_csv_auto('" + csv + "', HEADER=true)");
            st.execute("COPY entregas TO '" + out + "' (FORMAT PARQUET)");
        }
    }

    public static List<EntregaRaw> readEntregas(Path parquetFile) throws Exception {
        if (!Files.isRegularFile(parquetFile)) {
            throw new IllegalStateException("Parquet ausente: " + parquetFile);
        }
        String path = parquetFile.toAbsolutePath().toString().replace('\\', '/');
        List<EntregaRaw> out = new ArrayList<>();
        try (Connection c = DriverManager.getConnection("jdbc:duckdb:");
             PreparedStatement ps = c.prepareStatement("SELECT * FROM read_parquet(?)");
        ) {
            ps.setString(1, path);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new EntregaRaw(
                            str(rs, "id_entrega"),
                            str(rs, "regiao"),
                            str(rs, "data_conclusao"),
                            str(rs, "prazo_original"),
                            str(rs, "status_final"),
                            bool(rs, "atraso"),
                            str(rs, "cliente"),
                            str(rs, "unidade")));
                }
            }
        }
        return out;
    }

    private static String str(ResultSet rs, String col) throws Exception {
        String v = rs.getString(col);
        return v == null ? "" : v.trim();
    }

    private static boolean bool(ResultSet rs, String col) throws Exception {
        Object v = rs.getObject(col);
        if (v == null) {
            return false;
        }
        if (v instanceof Boolean b) {
            return b;
        }
        String s = String.valueOf(v).trim().toLowerCase();
        return "true".equals(s) || "1".equals(s) || "t".equals(s);
    }
}
