package com.mundodev.filafacil.api.repo;

import com.mundodev.filafacil.api.error.ApiValidationException;
import com.mundodev.filafacil.api.error.PersistenceFailureException;
import com.mundodev.filafacil.api.model.Agendamento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcAgendamentoRepository implements AgendamentoRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcAgendamentoRepository.class);

    private final DataSource dataSource;

    public JdbcAgendamentoRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void ensureSchema() {
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            st.execute(loadSchemaSql());
            log.info("Schema agendamentos verificado.");
        } catch (SQLException ex) {
            log.error("Falha ao aplicar schema (detalhe omitido por seguranca).");
            throw new PersistenceFailureException(ex);
        }
    }

    @Override
    public Agendamento insert(Agendamento agendamento) {
        String sql = """
                INSERT INTO agendamentos
                (transportadora, placa, doca, data_hora, status, anexo_ref, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                RETURNING id, transportadora, placa, doca, data_hora, status, anexo_ref, created_at, updated_at
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, agendamento.transportadora());
            ps.setString(2, agendamento.placa());
            ps.setString(3, agendamento.doca());
            ps.setTimestamp(4, Timestamp.valueOf(agendamento.dataHora()));
            ps.setString(5, agendamento.status());
            ps.setString(6, agendamento.anexoRef());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new PersistenceFailureException();
                }
                return map(rs);
            }
        } catch (SQLException ex) {
            log.error("Erro ao inserir agendamento.");
            throw new PersistenceFailureException(ex);
        }
    }

    @Override
    public List<Agendamento> findAll() {
        String sql = """
                SELECT id, transportadora, placa, doca, data_hora, status, anexo_ref, created_at, updated_at
                FROM agendamentos ORDER BY id
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Agendamento> list = new ArrayList<>();
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        } catch (SQLException ex) {
            log.error("Erro ao listar agendamentos.");
            throw new PersistenceFailureException(ex);
        }
    }

    @Override
    public List<Agendamento> findByDate(LocalDate data) {
        String sql = """
                SELECT id, transportadora, placa, doca, data_hora, status, anexo_ref, created_at, updated_at
                FROM agendamentos
                WHERE data_hora::date = ?
                ORDER BY id
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, data);
            try (ResultSet rs = ps.executeQuery()) {
                List<Agendamento> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            }
        } catch (SQLException ex) {
            log.error("Erro ao filtrar agendamentos por data.");
            throw new PersistenceFailureException(ex);
        }
    }

    @Override
    public Optional<Agendamento> findById(long id) {
        String sql = """
                SELECT id, transportadora, placa, doca, data_hora, status, anexo_ref, created_at, updated_at
                FROM agendamentos WHERE id = ?
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(map(rs));
            }
        } catch (SQLException ex) {
            log.error("Erro ao buscar agendamento.");
            throw new PersistenceFailureException(ex);
        }
    }

    @Override
    public Agendamento updateStatus(long id, String status) {
        String sql = """
                UPDATE agendamentos SET status = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                RETURNING id, transportadora, placa, doca, data_hora, status, anexo_ref, created_at, updated_at
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new ApiValidationException("Agendamento nao encontrado: " + id);
                }
                return map(rs);
            }
        } catch (ApiValidationException ex) {
            throw ex;
        } catch (SQLException ex) {
            log.error("Erro ao atualizar status.");
            throw new PersistenceFailureException(ex);
        }
    }

    @Override
    public Agendamento updateAnexoRef(long id, String anexoRef) {
        String sql = """
                UPDATE agendamentos SET anexo_ref = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                RETURNING id, transportadora, placa, doca, data_hora, status, anexo_ref, created_at, updated_at
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, anexoRef);
            ps.setLong(2, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new ApiValidationException("Agendamento nao encontrado: " + id);
                }
                return map(rs);
            }
        } catch (ApiValidationException ex) {
            throw ex;
        } catch (SQLException ex) {
            log.error("Erro ao atualizar anexo_ref.");
            throw new PersistenceFailureException(ex);
        }
    }

    private static Agendamento map(ResultSet rs) throws SQLException {
        return new Agendamento(
                rs.getLong("id"),
                rs.getString("transportadora"),
                rs.getString("placa"),
                rs.getString("doca"),
                rs.getTimestamp("data_hora").toLocalDateTime(),
                rs.getString("status"),
                rs.getString("anexo_ref"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime());
    }

    private static String loadSchemaSql() {
        try (InputStream in = JdbcAgendamentoRepository.class.getResourceAsStream("/api/schema.sql")) {
            if (in == null) {
                throw new IllegalStateException("schema.sql nao encontrado no classpath.");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao carregar schema.sql", ex);
        }
    }
}
