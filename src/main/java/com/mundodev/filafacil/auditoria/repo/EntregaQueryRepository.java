package com.mundodev.filafacil.auditoria.repo;

import com.mundodev.filafacil.auditoria.model.EntregaFiltro;
import com.mundodev.filafacil.auditoria.model.EntregaRow;
import com.mundodev.filafacil.auditoria.model.PageResult;
import com.mundodev.filafacil.auditoria.model.ResumoAuditoria;

import java.util.List;

public interface EntregaQueryRepository {

    PageResult<EntregaRow> listarPaginado(EntregaFiltro filtro, int page, int size);

    /** INNER JOIN entregas x clientes x motoristas. */
    List<EntregaRow> listarComClienteMotorista();

    /** LEFT JOIN: entregas mesmo sem ocorrencia. */
    List<EntregaRow> listarComOcorrenciaOpcional();

    /**
     * RIGHT JOIN (ou equivalente): motoristas e suas entregas (inclui motorista sem entrega no periodo).
     */
    List<String> motoristasComEntregasRightJoin();

    ResumoAuditoria calcularResumo();
}
