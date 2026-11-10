# Spec Delta

## Purpose

Garantir aprovacao de reembolso com consistencia transacional e indices documentados nos campos de consulta frequente.

## ADDED Requirements

### Requirement: Aprovacao em transacao
O fluxo de aprovacao de reembolso MUST executar em transacao atomica e MUST reverter (rollback) se qualquer etapa falhar.

#### Scenario: Rollback em falha
- **WHEN** ocorre erro durante a aprovacao apos iniciar a transacao
- **THEN** nenhuma alteracao parcial de reembolso/entrega relacionada permanece commitada

#### Scenario: Commit em sucesso
- **WHEN** a aprovacao conclui sem erro
- **THEN** o reembolso fica aprovado de forma persistente e visivel nas consultas seguintes

### Requirement: Indices em campos frequentes
O schema MUST incluir indices em status, data, cliente e motorista (ou equivalentes) e o README MUST justificar cada indice.

#### Scenario: Documentacao de indices
- **WHEN** um avaliador le o README do modulo
- **THEN** encontra a lista de indices e a justificativa de uso em filtros/joins
