package com.mundodev.filafacil.auditoria.repo;

import com.mundodev.filafacil.auditoria.model.EntregaFiltro;
import com.mundodev.filafacil.auditoria.model.EntregaRow;
import com.mundodev.filafacil.auditoria.model.PageResult;
import com.mundodev.filafacil.auditoria.model.ResumoAuditoria;
import com.mundodev.filafacil.error.EntradaException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class JdbcEntregaQueryRepository implements EntregaQueryRepository {

    private final DataSource dataSource;

    public JdbcEntregaQueryRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public PageResult<EntregaRow> listarPaginado(EntregaFiltro filtro, int page, int size) {
        if (page < 1 || size < 1) {
            throw new EntradaException("Pagina e tamanho devem ser >= 1.");
        }
        EntregaFiltro f = filtro == null ? EntregaFiltro.vazio() : filtro;
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        appendFilters(where, params, f);

        String countSql = """
                SELECT COUNT(*) FROM aud_entregas e
                INNER JOIN aud_clientes c ON c.id = e.cliente_id
                INNER JOIN aud_motoristas m ON m.id = e.motorista_id
                LEFT JOIN aud_ocorrencias o ON o.entrega_id = e.id
                LEFT JOIN aud_reembolsos r ON r.entrega_id = e.id
                """ + where;

        String dataSql = """
                SELECT e.id, e.status, e.data_entrega, e.valor_centavos,
                       c.id AS cliente_id, c.nome AS cliente_nome,
                       m.id AS motorista_id, m.nome AS motorista_nome,
                       o.tipo AS ocorrencia_tipo, r.status AS reembolso_status
                FROM aud_entregas e
                INNER JOIN aud_clientes c ON c.id = e.cliente_id
                INNER JOIN aud_motoristas m ON m.id = e.motorista_id
                LEFT JOIN aud_ocorrencias o ON o.entrega_id = e.id
                LEFT JOIN aud_reembolsos r ON r.entrega_id = e.id
                """ + where + " ORDER BY e.id LIMIT ? OFFSET ?";

        try (Connection conn = dataSource.getConnection()) {
            long total;
            try (PreparedStatement ps = conn.prepareStatement(countSql)) {
                bind(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    total = rs.getLong(1);
                }
            }
            List<EntregaRow> items = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(dataSql)) {
                List<Object> dataParams = new ArrayList<>(params);
                dataParams.add(size);
                dataParams.add((page - 1) * size);
                bind(ps, dataParams);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        items.add(mapRow(rs));
                    }
                }
            }
            return new PageResult<>(items, page, size, total);
        } catch (SQLException ex) {
            throw new IllegalStateException("Falha na consulta paginada de entregas.", ex);
        }
    }

    @Override
    public List<EntregaRow> listarComClienteMotorista() {
        String sql = """
                SELECT e.id, e.status, e.data_entrega, e.valor_centavos,
                       c.id AS cliente_id, c.nome AS cliente_nome,
                       m.id AS motorista_id, m.nome AS motorista_nome,
                       NULL AS ocorrencia_tipo, NULL AS reembolso_status
                FROM aud_entregas e
                INNER JOIN aud_clientes c ON c.id = e.cliente_id
                INNER JOIN aud_motoristas m ON m.id = e.motorista_id
                ORDER BY e.id
                """;
        return queryList(sql);
    }

    @Override
    public List<EntregaRow> listarComOcorrenciaOpcional() {
        String sql = """
                SELECT e.id, e.status, e.data_entrega, e.valor_centavos,
                       c.id AS cliente_id, c.nome AS cliente_nome,
                       m.id AS motorista_id, m.nome AS motorista_nome,
                       o.tipo AS ocorrencia_tipo, NULL AS reembolso_status
                FROM aud_entregas e
                INNER JOIN aud_clientes c ON c.id = e.cliente_id
                INNER JOIN aud_motoristas m ON m.id = e.motorista_id
                LEFT JOIN aud_ocorrencias o ON o.entrega_id = e.id
                ORDER BY e.id
                """;
        return queryList(sql);
    }

    @Override
    public List<String> motoristasComEntregasRightJoin() {
        // RIGHT JOIN: motoristas a direita; inclui motoristas sem entregas
        String sql = """
                SELECT m.id, m.nome, COUNT(e.id) AS qtd
                FROM aud_entregas e
                RIGHT JOIN aud_motoristas m ON m.id = e.motorista_id
                GROUP BY m.id, m.nome
                ORDER BY m.id
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<String> out = new ArrayList<>();
            while (rs.next()) {
                out.add(rs.getLong(1) + "|" + rs.getString(2) + "|entregas=" + rs.getLong(3));
            }
            return out;
        } catch (SQLException ex) {
            throw new IllegalStateException("Falha no RIGHT JOIN de motoristas.", ex);
        }
    }

    @Override
    public ResumoAuditoria calcularResumo() {
        String sql = """
                SELECT
                  (SELECT COUNT(*) FROM aud_entregas) AS total_entregas,
                  (SELECT COUNT(*) FROM aud_reembolsos WHERE status = 'pendente') AS pendentes,
                  (SELECT COUNT(*) FROM aud_reembolsos WHERE status = 'aprovado') AS aprovados
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return new ResumoAuditoria(rs.getLong(1), rs.getLong(2), rs.getLong(3), false);
        } catch (SQLException ex) {
            throw new IllegalStateException("Falha ao calcular resumo.", ex);
        }
    }

    private List<EntregaRow> queryList(String sql) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<EntregaRow> items = new ArrayList<>();
            while (rs.next()) {
                items.add(mapRow(rs));
            }
            return items;
        } catch (SQLException ex) {
            throw new IllegalStateException("Falha na query de entregas.", ex);
        }
    }

    private static void appendFilters(StringBuilder where, List<Object> params, EntregaFiltro f) {
        if (f.status() != null && !f.status().isBlank()) {
            where.append(" AND e.status = ? ");
            params.add(f.status().trim().toLowerCase());
        }
        if (f.dataInicio() != null) {
            where.append(" AND e.data_entrega >= ? ");
            params.add(Date.valueOf(f.dataInicio()));
        }
        if (f.dataFim() != null) {
            where.append(" AND e.data_entrega <= ? ");
            params.add(Date.valueOf(f.dataFim()));
        }
        if (f.clienteId() != null) {
            where.append(" AND e.cliente_id = ? ");
            params.add(f.clienteId());
        }
        if (f.motoristaId() != null) {
            where.append(" AND e.motorista_id = ? ");
            params.add(f.motoristaId());
        }
        if (f.temReembolso() != null) {
            if (f.temReembolso()) {
                where.append(" AND r.id IS NOT NULL ");
            } else {
                where.append(" AND r.id IS NULL ");
            }
        }
    }

    private static void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    private static EntregaRow mapRow(ResultSet rs) throws SQLException {
        return new EntregaRow(
                rs.getLong("id"),
                rs.getString("status"),
                rs.getDate("data_entrega").toLocalDate(),
                rs.getInt("valor_centavos"),
                rs.getLong("cliente_id"),
                rs.getString("cliente_nome"),
                rs.getLong("motorista_id"),
                rs.getString("motorista_nome"),
                rs.getString("ocorrencia_tipo"),
                rs.getString("reembolso_status"));
    }
}
