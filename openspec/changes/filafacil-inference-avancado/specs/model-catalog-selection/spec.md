# Spec Delta

## Purpose

Escolher o modelo pelo requisito da tarefa, com atributos de custo, qualidade, privacidade e tool use visíveis no catálogo.

## ADDED Requirements

### Requirement: Atributos do catalogo
O sistema MUST registrar, para cada modelo, qualidade esperada, custo, latência, janela de contexto, disponibilidade, suporte multimodal, privacidade, tool use, idiomas e uma limitação conhecida.

#### Scenario: Dois modelos
- **WHEN** o catálogo de modelos é carregado
- **THEN** existem ao menos um modelo barato sem tool use e um modelo robusto com tool use e privacidade maior

### Requirement: Selecao por tarefa
O sistema MUST usar o modelo barato na classificação simples e MUST usar o modelo robusto quando a tarefa exigir tool use, risco alto ou janela longa.

#### Scenario: Tool use
- **WHEN** a tarefa é resposta assistida com tool use
- **THEN** o modelo escolhido declara suporte a tool use

#### Scenario: Fallback compativel
- **WHEN** o modelo primário falha e o reserva tem privacidade menor que a exigida pela tarefa
- **THEN** o fallback é recusado e a chamada não segue nesse modelo
