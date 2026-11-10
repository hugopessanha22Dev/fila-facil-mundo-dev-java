# Spec Delta

## Purpose

Permite disparar e acompanhar o pipeline batch da FilaFácil pelo terminal ou CLI, sem interface web.

## ADDED Requirements

### Requirement: Execucao pelo terminal
O sistema SHALL oferecer uma opcao de menu ou comando CLI para iniciar o pipeline batch localmente.

#### Scenario: Usuario dispara o pipeline
- **WHEN** o operador escolhe a opcao de pipeline no menu (ou CLI equivalente)
- **THEN** o sistema inicia a orquestracao das etapas e exibe o resultado da execucao no terminal

### Requirement: Feedback de status no terminal
O sistema SHALL informar no terminal o status final da execucao (sucesso ou falha) e um resumo das contagens principais.

#### Scenario: Resumo apos sucesso
- **WHEN** o pipeline termina com sucesso
- **THEN** o terminal exibe status de sucesso e totais de registros lidos, descartados e gravados

### Requirement: Menus existentes preservados
A integracao do pipeline no terminal MUST preservar as opcoes de menu ja existentes (coletas, inference, manifesto, auditoria e demais).

#### Scenario: Menu anterior continua disponivel
- **WHEN** a opcao de pipeline e adicionada
- **THEN** as opcoes anteriores do menu permanecem acessiveis e funcionais
