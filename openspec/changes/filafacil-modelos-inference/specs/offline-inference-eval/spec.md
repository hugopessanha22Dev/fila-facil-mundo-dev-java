# Spec Delta

## Purpose

Permite avaliar a camada de inferência offline com golden dataset, métricas de qualidade/custo/latência/fallback e análise de falhas documentada para melhoria contínua.

## ADDED Requirements

### Requirement: Golden dataset
O projeto MUST incluir um golden dataset com mensagens simuladas (consulta, ocorrência, reclamação) e respostas esperadas estruturadas para comparação.

#### Scenario: Dataset mínimo
- **WHEN** a avaliação offline é iniciada
- **THEN** o runner carrega pelo menos um conjunto de exemplos cobrindo os três tipos de solicitação

### Requirement: Métricas de avaliação
A avaliação offline MUST reportar pelo menos: taxa de JSON válido; acurácia/campos corretos frente ao esperado; latência; custo estimado; taxa de fallback.

#### Scenario: Relatório após run
- **WHEN** o runner conclui o golden dataset
- **THEN** um relatório (console ou arquivo) apresenta as métricas exigidas

### Requirement: Análise de falhas
A entrega MUST documentar onde o modelo errou, impacto operacional e ajuste sugerido (prompt, roteamento, parâmetros ou schema).

#### Scenario: Caso falho no golden
- **WHEN** um exemplo do golden dataset falha validação ou diverge do esperado
- **THEN** a análise de falhas registra o caso, a causa provável e um ajuste recomendado
