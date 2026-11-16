# Spec Delta

## Purpose

Dar a cada fluxo de IA da FilaFácil requisitos próprios de qualidade, custo, latência, risco, idioma, contexto, tool use e privacidade.

## ADDED Requirements

### Requirement: Cinco tarefas independentes
O sistema MUST registrar classificação, extração, resumo, resposta assistida e busca contextual, e MUST guardar para cada uma qualidade mínima, teto de custo, teto de latência, risco, idioma, janela de contexto, necessidade de tool use e nível de privacidade.

#### Scenario: Classificacao barata
- **WHEN** o catálogo de tarefas é lido
- **THEN** a classificação consta como risco baixo, idioma pt-BR, sem tool use e com janela curta

#### Scenario: Extracao deterministica
- **WHEN** o catálogo de tarefas é lido
- **THEN** a extração consta como risco alto, com saída JSON e sem streaming

### Requirement: Busca contextual sem cache livre
O sistema MUST marcar a busca contextual como tarefa de contexto longo e de groundedness obrigatória, e MUST marcar o cache dessa tarefa como proibido.

#### Scenario: Cache da busca
- **WHEN** a política da busca contextual é consultada
- **THEN** o cache permitido é falso
