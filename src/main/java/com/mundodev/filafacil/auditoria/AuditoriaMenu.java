package com.mundodev.filafacil.auditoria;

import com.mundodev.filafacil.auditoria.model.AuditEvent;
import com.mundodev.filafacil.auditoria.model.EntregaFiltro;
import com.mundodev.filafacil.auditoria.model.EntregaRow;
import com.mundodev.filafacil.auditoria.model.PageResult;
import com.mundodev.filafacil.auditoria.model.ResumoAuditoria;
import com.mundodev.filafacil.auditoria.service.AuditoriaService;
import com.mundodev.filafacil.error.EntradaException;

import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

public class AuditoriaMenu {

    private final AuditoriaService service;
    private final Scanner scanner;

    public AuditoriaMenu(AuditoriaService service, Scanner scanner) {
        this.service = service;
        this.scanner = scanner;
    }

    public void executar() {
        boolean ativo = true;
        while (ativo) {
            System.out.println("--- Auditoria de Entregas/Reembolsos ---");
            System.out.println("1 - Listar entregas paginadas");
            System.out.println("2 - Demo joins (INNER / LEFT / RIGHT)");
            System.out.println("3 - Aprovar reembolso");
            System.out.println("4 - Resumo cacheado");
            System.out.println("5 - Eventos NoSQL");
            System.out.println("0 - Voltar");
            String op = ler("Opcao auditoria: ");
            try {
                switch (op) {
                    case "1" -> listar();
                    case "2" -> demoJoins();
                    case "3" -> aprovar();
                    case "4" -> resumo();
                    case "5" -> eventos();
                    case "0" -> ativo = false;
                    default -> System.out.println("Opcao invalida.");
                }
            } catch (Exception ex) {
                System.out.println("ERRO auditoria: " + ex.getMessage());
            }
            System.out.println();
        }
    }

    private void listar() {
        int page = lerInt("Pagina [1]: ", 1);
        int size = lerInt("Tamanho [5]: ", 5);
        String status = blankToNull(ler("Status (vazio=todos): "));
        Long clienteId = lerLongOpt("Cliente id (vazio=todos): ");
        Long motoristaId = lerLongOpt("Motorista id (vazio=todos): ");
        EntregaFiltro filtro = new EntregaFiltro(status, null, null, clienteId, motoristaId, null);
        PageResult<EntregaRow> result = service.listarEntregas(filtro, page, size);
        System.out.println("Total=" + result.total() + " pagina=" + result.page()
                + "/" + Math.max(1, result.totalPages()) + " next=" + result.hasNext());
        for (EntregaRow row : result.items()) {
            System.out.println(formatRow(row));
        }
    }

    private void demoJoins() {
        System.out.println("INNER JOIN (entregas com cliente/motorista): "
                + service.demoInnerJoin().size() + " linhas");
        long semOcorrencia = service.demoLeftJoin().stream()
                .filter(r -> r.ocorrenciaTipo() == null).count();
        System.out.println("LEFT JOIN: entregas sem ocorrencia = " + semOcorrencia);
        System.out.println("RIGHT JOIN motoristas:");
        for (String line : service.demoRightJoin()) {
            System.out.println("  " + line);
        }
    }

    private void aprovar() {
        long id = lerLong("Id do reembolso: ");
        var novo = service.aprovarReembolso(id);
        System.out.println("Reembolso aprovado: " + novo);
    }

    private void resumo() {
        ResumoAuditoria r = service.resumoCacheado();
        System.out.println("entregas=" + r.totalEntregas()
                + " pendentes=" + r.totalReembolsosPendentes()
                + " aprovados=" + r.totalReembolsosAprovados()
                + " fromCache=" + r.fromCache());
    }

    private void eventos() {
        List<AuditEvent> events = service.listarEventos(20);
        if (events.isEmpty()) {
            System.out.println("Nenhum evento de auditoria.");
            return;
        }
        for (AuditEvent e : events) {
            System.out.println(e.em() + " [" + e.tipo() + "] " + e.entidade() + "#" + e.entidadeId()
                    + " user=" + e.usuario());
        }
    }

    private static String formatRow(EntregaRow r) {
        return "id=" + r.id()
                + " status=" + r.status()
                + " data=" + r.dataEntrega()
                + " cliente=" + r.clienteNome()
                + " motorista=" + r.motoristaNome()
                + " ocorrencia=" + r.ocorrenciaTipo()
                + " reembolso=" + r.reembolsoStatus();
    }

    private String ler(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }

    private int lerInt(String prompt, int def) {
        String raw = ler(prompt);
        if (raw == null || raw.isBlank()) {
            return def;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            throw new EntradaException("Numero invalido: " + raw);
        }
    }

    private long lerLong(String prompt) {
        String raw = ler(prompt);
        try {
            return Long.parseLong(raw.trim());
        } catch (Exception ex) {
            throw new EntradaException("Id invalido.");
        }
    }

    private Long lerLongOpt(String prompt) {
        String raw = ler(prompt);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ex) {
            throw new EntradaException("Id invalido: " + raw);
        }
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
