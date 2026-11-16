# Spec Delta

## Purpose

Aplicar batching, cache, compressão e poda só quando a tarefa permite, registrando o risco de cada uma.

## ADDED Requirements

### Requirement: Criterio de cada otimizacao
O sistema MUST registrar, para batching, cache, compressão de prompt e poda de contexto, quando usar, quando não usar e o risco.

#### Scenario: Politica presente
- **WHEN** a política de otimização é lida
- **THEN** as quatro estratégias têm uso, proibição e risco preenchidos

### Requirement: Cache e poda seletivos
O sistema MUST cachear classificação de texto idêntico e MUST NOT cachear busca contextual. A poda MUST manter a evidência marcada como crítica.

#### Scenario: Busca sem cache
- **WHEN** a mesma pergunta contextual é repetida
- **THEN** a segunda passagem não reutiliza resposta em cache

#### Scenario: Evidencia preservada
- **WHEN** o contexto longo contém o marcador crítico e passa pela poda
- **THEN** o texto enviado ao modelo ainda contém esse marcador
