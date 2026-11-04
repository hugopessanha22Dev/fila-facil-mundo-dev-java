package com.mundodev.filafacil.controller;

import com.mundodev.filafacil.model.Coleta;
import com.mundodev.filafacil.service.ColetaService;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class MenuController {

    private final ColetaService service;
    private final Scanner scanner;

    public MenuController(ColetaService service, Scanner scanner) {
        this.service = service;
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
                    case "0" -> {
                        System.out.println("Encerrando FilaFacil. Ate logo!");
                        rodando = false;
                    }
                    default -> System.out.println("Opcao invalida. Tente novamente.");
                }
            } catch (IllegalArgumentException ex) {
                System.out.println("ERRO: " + ex.getMessage());
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
        System.out.println("0 - Sair");
        System.out.println("=================================");
    }

    private void cadastrar() {
        System.out.println("--- Cadastro de coleta ---");
        String nome = lerLinha("Nome do cliente: ");
        String endereco = lerLinha("Endereco: ");
        Integer pacotes = lerInteiro("Quantidade de pacotes: ");
        String prioridade = lerLinha("Prioridade (baixa, media, alta): ");

        Coleta salva = service.cadastrar(nome, endereco, pacotes, prioridade);
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
            default -> throw new IllegalArgumentException("Tipo de filtro invalido.");
        };
        imprimirLista(resultado);
    }

    private void atualizarStatus() {
        System.out.println("--- Atualizar status ---");
        Long id = lerLong("Id da coleta: ");
        String status = lerLinha("Novo status (pendente, em_rota, concluida): ");
        service.atualizarStatus(id, status);
        System.out.println("SUCESSO: status atualizado para a coleta " + id + ".");
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
            throw new IllegalArgumentException("Quantidade de pacotes e obrigatoria.");
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Valor numerico invalido: " + raw);
        }
    }

    private Long lerLong(String prompt) {
        String raw = lerLinha(prompt);
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalArgumentException("Id invalido.");
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Id invalido: " + raw);
        }
    }
}
