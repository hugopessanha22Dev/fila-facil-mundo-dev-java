package com.mundodev.filafacil.auditoria.repo;

import com.mundodev.filafacil.error.EntradaException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class JdbcReembolsoRepository implements ReembolsoRepository {

    private final DataSource dataSource;

    public JdbcReembolsoRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<Map<String, Object>> findById(long id) {
        String sql = "SELECT id, entrega_id, status, motivo, valor_centavos FROM aud_reembolsos WHERE id = ?";
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
            throw new IllegalStateException("Falha ao buscar reembolso.", ex);
        }
    }

    @Override
    public Map<String, Object> aprovarEmTransacao(long id, boolean failAfterLock) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Map<String, Object> antigo;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT id, entrega_id, status, motivo, valor_centavos FROM aud_reembolsos WHERE id = ? FOR UPDATE")) {
                    ps.setLong(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new EntradaException("Reembolso nao encontrado: " + id);
                        }
                        antigo = map(rs);
                    }
                }
                if (!"pendente".equals(String.valueOf(antigo.get("status")))) {
                    throw new EntradaException("Reembolso nao esta pendente.");
                }
                if (failAfterLock) {
                    throw new IllegalStateException("Falha simulada na aprovacao (rollback).");
                }
                try (PreparedStatement upd = conn.prepareStatement(
                        "UPDATE aud_reembolsos SET status = 'aprovado', atualizado_em = CURRENT_TIMESTAMP WHERE id = ?")) {
                    upd.setLong(1, id);
                    upd.executeUpdate();
                }
                conn.commit();
                return antigo;
            } catch (Exception ex) {
                conn.rollback();
                if (ex instanceof RuntimeException re) {
                    throw re;
                }
                throw new IllegalStateException("Falha na aprovacao de reembolso.", ex);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Falha de conexao na aprovacao.", ex);
        }
    }

    private static Map<String, Object> map(ResultSet rs) throws SQLException {
        Map<String, Object> m = new HashMap<>();
        m.put("id", rs.getLong("id"));
        m.put("entregaId", rs.getLong("entrega_id"));
        m.put("status", rs.getString("status"));
        m.put("motivo", rs.getString("motivo"));
        m.put("valorCentavos", rs.getInt("valor_centavos"));
        return m;
    }
}
