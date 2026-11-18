# Spec Delta

## Purpose

Registrar a ficha operacional de cada fluxo de IA monitorado, com dono, risco, métrica e a consequência de ficar abaixo do limite.

## ADDED Requirements

### Requirement: Quatro fluxos catalogados
O sistema MUST catalogar atendimento, RAG de políticas, agente de ocorrência e classificador, cada um com dono, risco, modelo, prompt, entrada, saída, ferramentas e fonte de grounding.

#### Scenario: Ficha completa
- **WHEN** o catálogo é consultado
- **THEN** os quatro fluxos estão presentes e cada um traz dono e critério de bloqueio

### Requirement: Metrica com consequencia
O sistema MUST dar a cada métrica uma regra de cálculo, um limite aceitável, um dono e a consequência de ficar abaixo do limite.

#### Scenario: Groundedness abaixo do limite
- **WHEN** a groundedness de um fluxo fica abaixo do limite
- **THEN** a consequência registrada é bloqueio
