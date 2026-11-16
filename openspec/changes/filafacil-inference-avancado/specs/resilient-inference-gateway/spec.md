# Spec Delta

## Purpose

Fazer toda chamada de IA passar pelo gateway, com limite de taxa, retry, timeout, circuit breaker, validação de saída e métricas.

## ADDED Requirements

### Requirement: Entrada unica
O sistema MUST recusar uma chamada de modelo que não passe pelo gateway central.

#### Scenario: Atalho direto
- **WHEN** um fluxo pede inferência
- **THEN** a execução registrada traz o nome do gateway e a tarefa

### Requirement: Limite, retry e circuito
O sistema MUST aplicar rate limit por provedor e por tarefa, retry com limite e backoff, timeout da tarefa, e circuit breaker que abre acima do limiar, testa em half-open e fecha após sucesso.

#### Scenario: Circuito aberto
- **WHEN** as falhas do provedor atingem o limiar
- **THEN** a chamada seguinte não consulta o provedor e a métrica registra circuito aberto

#### Scenario: Rate limit
- **WHEN** a tarefa já atingiu o limite de chamadas da janela
- **THEN** a chamada é recusada antes do provedor

### Requirement: Saida invalida e metricas
O sistema MUST tratar JSON inválido de extração ou tool use com retry ou fallback antes de devolver erro ao produto, e MUST contar latência, custo, tokens, erro, fallback e tipo de falha por provedor e por tarefa.

#### Scenario: Json invalido
- **WHEN** a extração devolve texto que não é JSON
- **THEN** ocorre ao menos mais uma tentativa ou um fallback, e o produto não recebe esse texto como resultado aceito
