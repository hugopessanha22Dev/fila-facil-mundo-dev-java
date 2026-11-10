package com.mundodev.filafacil.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mundodev.filafacil.error.AppException;
import com.mundodev.filafacil.error.EntradaException;
import com.mundodev.filafacil.inference.OperationalTriageService;
import com.mundodev.filafacil.inference.TriageResult;
import com.mundodev.filafacil.inference.prompt.ConversationOrchestrator;
import com.mundodev.filafacil.auditoria.AuditoriaMenu;
import com.mundodev.filafacil.auditoria.service.AuditoriaService;
import com.mundodev.filafacil.pipeline.PipelineConfig;
import com.mundodev.filafacil.pipeline.PipelineModule;
import com.mundodev.filafacil.pipeline.PipelineRunLog;
import com.mundodev.filafacil.pipeline.inter.InterConfig;
import com.mundodev.filafacil.pipeline.inter.InterMode;
import com.mundodev.filafacil.pipeline.inter.InterRunReport;
import com.mundodev.filafacil.pipeline.inter.LatePolicy;
import com.mundodev.filafacil.pipeline.inter.PipelineInterModule;
import com.mundodev.filafacil.coletas.Coleta;
import com.mundodev.filafacil.coletas.ColetaService;
import com.mundodev.filafacil.manifest.ManifestValidationService;
import com.mundodev.filafacil.manifest.ValidationReport;
import com.mundodev.filafacil.state.AppState;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class MenuController {

    private final ColetaService service;
    private final OperationalTriageService triageService;
    private final ConversationOrchestrator conversationOrchestrator;
    private final ManifestValidationService manifestValidationService;
    private final AuditoriaService auditoriaService;
    private final Scanner scanner;
    private final ObjectMapper mapper = new ObjectMapper();

    public MenuController(
            ColetaService service,
            OperationalTriageService triageService,
            ConversationOrchestrator conversationOrchestrator,
            Scanner scanner) {
        this(service, triageService, conversationOrchestrator, new ManifestValidationService(), null, scanner);
    }

    public MenuController(
            ColetaService service,
            OperationalTriageService triageService,
            ConversationOrchestrator conversationOrchestrator,
            AuditoriaService auditoriaService,
            Scanner scanner) {
        this(service, triageService, conversationOrchestrator, new ManifestValidationService(), auditoriaService, scanner);
    }

    public MenuController(
            ColetaService service,
            OperationalTriageService triageService,
            ConversationOrchestrator conversationOrchestrator,
            ManifestValidationService manifestValidationService,
            AuditoriaService auditoriaService,
            Scanner scanner) {
        this.service = service;
        this.triageService = triageService;
        this.conversationOrchestrator = conversationOrchestrator;
        this.manifestValidationService = manifestValidationService;
        this.auditoriaService = auditoriaService;
        this.scanner = scanner;
    }

    public void iniciar() {
        boolean rodando = true;
        while (rodando) {
            exibirMenu();
            String opcao = lerLinha("Escolha uma opcao: ");
            try {
                switch (opcao) {
                    case "1" -> cadastrar();
                    case "2" -> listar();
                    case "3" -> filtrar();
                    case "4" -> atualizarStatus();
                    case "5" -> resumir();
                    case "6" -> triar();
                    case "7" -> atendimentoConversacional();
                    case "8" -> validarManifesto();
                    case "9" -> deletarColeta();
                    case "10" -> auditoria();
                    case "11" -> pipelineBatch();
                    case "12" -> pipelineInter();
                    case "0" -> {
                        System.out.println("Encerrando FilaFacil. Ate logo!");
                        rodando = false;
                    }
                    default -> System.out.println("Opcao invalida. Tente novamente.");
                }
            } catch (AppException ex) {
                service.getState().setUltimoErro(ex.getOrigem(), ex.getMessage());
                System.out.println("ERRO [" + ex.getOrigem() + "]: " + ex.getMessage());
            } catch (NoSuchElementException ex) {
                System.out.println("Entrada encerrada. Saindo...");
                rodando = false;
            } catch (Exception ex) {
                System.out.println("ERRO inesperado: " + ex.getMessage());
            }
            if (rodando) {
                System.out.println();
            }
        }
    }

    private void exibirMenu() {
        System.out.println("=================================");
        System.out.println("   FilaFacil Logistica - Menu");
        System.out.println("=================================");
        System.out.println("1 - Cadastrar coleta");
        System.out.println("2 - Listar coletas");
        System.out.println("3 - Filtrar coletas");
        System.out.println("4 - Atualizar status");
        System.out.println("5 - Resumo operacional");
        System.out.println("6 - Triagem IA");
        System.out.println("7 - Atendimento conversacional");
        System.out.println("8 - Validar manifesto");
        System.out.println("9 - Deletar coleta");
        System.out.println("10 - Auditoria entregas/reembolsos");
        System.out.println("11 - Pipeline batch (ETL)");
        System.out.println("12 - Pipeline inter (CDC/incremental)");
        System.out.println("0 - Sair");
        System.out.println("=================================");
        AppState.AppError erro = service.getState().getUltimoErro();
        if (erro != null) {
            System.out.println("Ultimo erro [" + erro.origem() + "]: " + erro.mensagem());
        }
    }

    private void cadastrar() {
        System.out.println("--- Cadastro de coleta ---");
        String nome = lerLinha("Nome do cliente: ");
        String endereco = lerLinha("Endereco: ");
        Integer pacotes = lerInteiro("Quantidade de pacotes: ");
        String prioridade = lerLinha("Prioridade (baixa, media, alta): ");
        String cep = lerLinha("CEP (somente numeros ou 00000-000): ");

        System.out.println("Consultando ViaCEP...");
        Coleta salva = service.cadastrar(nome, endereco, pacotes, prioridade, cep);
        System.out.println("SUCESSO: coleta cadastrada com id " + salva.getId() + " (status pendente).");
        System.out.println(salva);
    }

    private void listar() {
        System.out.println("--- Listagem ---");
        imprimirLista(service.listar());
    }

    private void filtrar() {
        System.out.println("--- Filtros ---");
        System.out.println("1 - Por status");
        System.out.println("2 - Por prioridade");
        String escolha = lerLinha("Tipo de filtro: ");
        List<Coleta> resultado = switch (escolha) {
            case "1" -> service.filtrarPorStatus(lerLinha("Status (pendente, em_rota, concluida): "));
            case "2" -> service.filtrarPorPrioridade(lerLinha("Prioridade (baixa, media, alta): "));
            default -> throw new EntradaException("Tipo de filtro invalido.");
        };
        AppState.FiltrosAtivos filtros = service.getState().getFiltros();
        System.out.println("Filtros ativos: status=" + filtros.status() + ", prioridade=" + filtros.prioridade());
        imprimirLista(resultado);
    }

    private void atualizarStatus() {
        System.out.println("--- Atualizar status ---");
        Long id = lerLong("Id da coleta: ");
        String status = lerLinha("Novo status (pendente, em_rota, concluida): ");
        service.atualizarStatus(id, status);
        System.out.println("SUCESSO: status atualizado para a coleta " + id + ".");
    }

    private void deletarColeta() {
        System.out.println("--- Deletar coleta ---");
        Long id = lerLong("Id da coleta: ");
        service.deletar(id);
        System.out.println("SUCESSO: coleta " + id + " removida.");
    }

    private void auditoria() {
        if (auditoriaService == null) {
            System.out.println("Modulo de auditoria desligado (FILAFACIL_AUDITORIA=off).");
            return;
        }
        new AuditoriaMenu(auditoriaService, scanner).executar();
    }

    private void pipelineBatch() {
        System.out.println("--- Pipeline batch ---");
        try {
            PipelineConfig config = PipelineConfig.fromEnv();
            PipelineModule.PipelineRunResult result = PipelineModule.run(config);
            PipelineRunLog log = result.log();
            System.out.println(log.resumo());
            log.tempoMsPorEtapa().forEach((k, v) -> System.out.println("  " + k + ": " + v + " ms"));
            System.out.println("Log: " + result.context().paths().logFile(log.runId()).toAbsolutePath());
        } catch (Exception ex) {
            throw new EntradaException("Falha ao executar pipeline: " + ex.getMessage());
        }
    }

    private void pipelineInter() {
        System.out.println("--- Pipeline inter (CDC/incremental) ---");
        String modo = lerLinha("Modo [incremental|batch] (default incremental): ");
        InterMode mode = InterMode.parse(modo == null || modo.isBlank() ? "incremental" : modo);
        try {
            PipelineConfig base = PipelineConfig.fromEnv();
            InterConfig inter = new InterConfig(
                    base.pipelineRoot(),
                    mode,
                    LatePolicy.parse(System.getenv().getOrDefault("FILAFACIL_PIPELINE_LATE_POLICY", "mark")),
                    4,
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    java.util.Optional.empty(),
                    false);
            PipelineInterModule.InterRunResult result = PipelineInterModule.run(inter);
            InterRunReport report = result.report();
            System.out.println(report.resumo());
            report.lidosPorFonte().forEach((k, v) -> System.out.println("  fonte " + k + ": lidos=" + v));
            System.out.println("Lineage: " + result.lineageFile().toAbsolutePath());
            System.out.println("Consolidada: " + result.consolidadaState().toAbsolutePath());
        } catch (Exception ex) {
            throw new EntradaException("Falha ao executar pipeline inter: " + ex.getMessage());
        }
    }

    private void resumir() {
        System.out.println("--- Resumo operacional ---");
        ColetaService.ResumoOperacional resumo = service.resumir();
        System.out.println("Total de coletas: " + resumo.totalColetas());
        System.out.println("Total de pacotes: " + resumo.totalPacotes());
        System.out.println("Por prioridade:");
        for (Map.Entry<String, Long> entry : resumo.porPrioridade().entrySet()) {
            System.out.println("  - " + entry.getKey() + ": " + entry.getValue());
        }
        System.out.println("Por status:");
        for (Map.Entry<String, Long> entry : resumo.porStatus().entrySet()) {
            System.out.println("  - " + entry.getKey() + ": " + entry.getValue());
        }
    }

    private void triar() {
        System.out.println("--- Triagem IA ---");
        System.out.println("Cole a mensagem operacional. Linha vazia finaliza a entrada.");
        String mensagem = lerMultilinha();
        OperationalTriageService.TriageOutcome outcome = triageService.triar(mensagem);
        TriageResult result = outcome.result();
        System.out.println("Perfil: " + outcome.response().profileUsed()
                + " | modelo: " + outcome.response().modelId()
                + " | fallback=" + outcome.response().usedFallback()
                + " | latencyMs=" + outcome.response().latencyMs()
                + " | costUsd~" + outcome.response().estimatedCostUsd());
        try {
            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));
        } catch (Exception ex) {
            System.out.println(result);
        }
        if (!outcome.toolContext().isEmpty()) {
            System.out.println("Contexto tools: " + outcome.toolContext());
        }
    }

    private void validarManifesto() {
        System.out.println("--- Validar manifesto ---");
        String caminho = lerLinha("Caminho do manifesto (.json|.csv) [data/manifestos/valido.json]: ");
        if (caminho == null || caminho.isBlank()) {
            caminho = "data/manifestos/valido.json";
        }
        ValidationReport report = manifestValidationService.validar(Path.of(caminho.trim()));
        System.out.print(report.render());
        String salvar = lerLinha("Salvar relatorio em arquivo? (s/N): ");
        if (salvar != null && salvar.trim().equalsIgnoreCase("s")) {
            String destino = lerLinha("Arquivo de saida [data/manifestos/ultimo-relatorio.txt]: ");
            if (destino == null || destino.isBlank()) {
                destino = "data/manifestos/ultimo-relatorio.txt";
            }
            Path out = Path.of(destino.trim());
            manifestValidationService.salvarRelatorio(report, out);
            System.out.println("Relatorio salvo em: " + out.toAbsolutePath());
        }
    }

    private void atendimentoConversacional() {
        System.out.println("--- Atendimento conversacional ---");
        System.out.println("Digite mensagens do caso. Comandos: /fim para encerrar, /reset para limpar sessao.");
        conversationOrchestrator.reset();
        boolean ativo = true;
        while (ativo) {
            String mensagem = lerLinha("voce> ");
            if (mensagem == null) {
                break;
            }
            String trimmed = mensagem.trim();
            if (trimmed.equalsIgnoreCase("/fim")) {
                conversationOrchestrator.reset();
                System.out.println("Sessao encerrada.");
                break;
            }
            if (trimmed.equalsIgnoreCase("/reset")) {
                conversationOrchestrator.reset();
                System.out.println("Sessao limpa.");
                continue;
            }
            ConversationOrchestrator.TurnResult turn = conversationOrchestrator.handleTurn(mensagem);
            if (!turn.concluded()) {
                System.out.println("ia> " + turn.clarificationQuestion());
            } else {
                System.out.println("ia> Conclusao estruturada:");
                System.out.println(turn.conclusionJson());
                if (turn.response() != null) {
                    System.out.println("perfil=" + turn.response().profileUsed()
                            + " fallback=" + turn.response().usedFallback());
                }
                conversationOrchestrator.reset();
                ativo = false;
            }
        }
    }

    private String lerMultilinha() {
        StringBuilder sb = new StringBuilder();
        while (true) {
            String line = scanner.nextLine();
            if (line == null || line.isEmpty()) {
                break;
            }
            if (!sb.isEmpty()) {
                sb.append('\n');
            }
            sb.append(line);
        }
        return sb.toString();
    }

    private void imprimirLista(List<Coleta> coletas) {
        if (coletas.isEmpty()) {
            System.out.println("Nenhuma coleta encontrada.");
            return;
        }
        System.out.println("Total: " + coletas.size());
        for (Coleta coleta : coletas) {
            System.out.println(coleta);
        }
    }

    private String lerLinha(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }

    private Integer lerInteiro(String prompt) {
        String raw = lerLinha(prompt);
        if (raw == null || raw.trim().isEmpty()) {
            throw new EntradaException("Quantidade de pacotes e obrigatoria.");
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            throw new EntradaException("Valor numerico invalido: " + raw);
        }
    }

    private Long lerLong(String prompt) {
        String raw = lerLinha(prompt);
        if (raw == null || raw.trim().isEmpty()) {
            throw new EntradaException("Id invalido.");
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ex) {
            throw new EntradaException("Id invalido: " + raw);
        }
    }
}
