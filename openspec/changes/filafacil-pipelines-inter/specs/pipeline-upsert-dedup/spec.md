# Spec Delta

## Purpose

Garante estado confiavel na camada final via deduplicacao por chave de negocio e upsert/merge que preserva a versao mais recente.

## ADDED Requirements

### Requirement: Deduplicacao por chave de negocio
O sistema SHALL deduplicar registros pela chave de negocio, usando timestamp de atualizacao e prioridade da fonte para escolher a versao vencedora.

#### Scenario: Mesma coleta em duas fontes
- **WHEN** a mesma chave de negocio aparece em duas fontes com timestamps diferentes
- **THEN** permanece a versao com timestamp mais recente (ou maior prioridade de fonte em empate)

### Requirement: Upsert ou merge na camada final
O sistema SHALL aplicar upsert/merge na camada final para inserir novos registros e atualizar existentes, refletindo o estado mais confiavel.

#### Scenario: Registro existente atualizado
- **WHEN** um registro ja consolidado recebe uma versao mais recente
- **THEN** o registro final e atualizado e a contagem de atualizados aumenta

#### Scenario: Registro novo inserido
- **WHEN** a chave de negocio ainda nao existe na camada final
- **THEN** o registro e inserido e a contagem de inseridos aumenta

### Requirement: Versao antiga nao sobrescreve
O sistema MUST NAO permitir que uma versao mais antiga sobrescreva dados validos mais recentes ja consolidados.

#### Scenario: CDC atrasado com versao menor
- **WHEN** chega um update com timestamp/versao inferior ao consolidado
- **THEN** o consolidado permanece inalterado e o evento e contado/descartado de forma rastreavel
