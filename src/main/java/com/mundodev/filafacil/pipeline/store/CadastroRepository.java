package com.mundodev.filafacil.pipeline.store;

import com.mundodev.filafacil.pipeline.PipelineConfig;
import com.mundodev.filafacil.pipeline.io.SourceReaders;
import com.mundodev.filafacil.pipeline.model.ClienteUnidade;
import com.mundodev.filafacil.pipeline.model.MetricaDiaria;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CadastroRepository implements AutoCloseable {

    public record CadastroLoad(Map<String, ClienteUnidade> rows, boolean fromJdbc) {
    }

    private final HikariDataSource dataSource;
    private final Path fallbackCsv;

    private CadastroRepository(HikariDataSource dataSource, Path fallbackCsv) {
        this.dataSource = dataSource;
        this.fallbackCsv = fallbackCsv;
    }

    public static CadastroRepository open(PipelineConfig config, Path fallbackCsv) {
        if (config.memoryDb()) {
            return new CadastroRepository(null, fallbackCsv);
        }
        try {
            HikariConfig hc = new HikariConfig();
            hc.setJdbcUrl(config.dbUrl());
            hc.setUsername(config.dbUser());
            hc.setPassword(config.dbPassword());
            hc.setMaximumPoolSize(2);
            hc.setPoolName("filafacil-pipeline");
            HikariDataSource ds = new HikariDataSource(hc);
            try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
                st.execute("SELECT 1");
            }
            return new CadastroRepository(ds, fallbackCsv);
        } catch (Exception ex) {
            return new CadastroRepository(null, fallbackCsv);
        }
    }

    public boolean jdbcAvailable() {
        return dataSource != null;
    }

    public CadastroLoad loadCadastro() throws Exception {
        if (dataSource != null) {
            Map<String, ClienteUnidade> out = new LinkedHashMap<>();
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "SELECT cliente_codigo, cliente_nome, unidade, regiao FROM pipe_clientes_unidades");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ClienteUnidade cu = new ClienteUnidade(
                            rs.getString("cliente_codigo"),
                            rs.getString("cliente_nome"),
                            rs.getString("unidade"),
                            rs.getString("regiao"));
                    out.put(cu.clienteCodigo(), cu);
                }
            } catch (Exception ignored) {
                // tabela ausente -> fallback
            }
            if (!out.isEmpty()) {
                return new CadastroLoad(out, true);
            }
        }
        return new CadastroLoad(SourceReaders.readClientesCsv(fallbackCsv), false);
    }

    public Optional<String> loadMetricas(List<MetricaDiaria> metricas, String runId) {
        if (dataSource == null) {
            return Optional.empty();
        }
        String sql = """
                INSERT INTO pipe_metricas_diarias
                (data_ref, regiao, status, cliente_codigo, total_coletas, total_entregas, total_atrasos, total_ocorrencias, run_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (data_ref, regiao, status, cliente_codigo, run_id) DO UPDATE SET
                  total_coletas = EXCLUDED.total_coletas,
                  total_entregas = EXCLUDED.total_entregas,
                  total_atrasos = EXCLUDED.total_atrasos,
                  total_ocorrencias = EXCLUDED.total_ocorrencias
                """;
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (MetricaDiaria m : metricas) {
                ps.setDate(1, Date.valueOf(m.dataRef()));
                ps.setString(2, m.regiao());
                ps.setString(3, m.status());
                ps.setString(4, m.clienteCodigo());
                ps.setInt(5, (int) m.totalColetas());
                ps.setInt(6, (int) m.totalEntregas());
                ps.setInt(7, (int) m.totalAtrasos());
                ps.setInt(8, (int) m.totalOcorrencias());
                ps.setString(9, runId);
                ps.addBatch();
            }
            ps.executeBatch();
            return Optional.of("pipe_metricas_diarias");
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    @Override
    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
