# Spec Delta

## Purpose

Acelerar consultas frequentes de auditoria com Redis, usando chave previsivel, expiracao e invalidacao ao atualizar dados relevantes.

## ADDED Requirements

### Requirement: Cache com chave e TTL
O sistema MUST cachear ao menos uma consulta frequente no Redis com chave definida e tempo de expiracao.

#### Scenario: Hit de cache
- **WHEN** a mesma consulta cacheavel e executada duas vezes dentro do TTL sem invalidacao
- **THEN** a segunda leitura pode ser servida do cache sem reconsultar o banco (comportamento observavel/logado)

### Requirement: Invalidacao ao atualizar
Ao aprovar reembolso ou alterar dados que afetam o resumo/consulta cacheada, o sistema MUST invalidar (ou atualizar) a chave de cache correspondente.

#### Scenario: Invalidacao apos aprovacao
- **WHEN** um reembolso e aprovado com sucesso
- **THEN** a chave de cache da consulta/resumo afetado e invalidada antes da proxima leitura obrigatoria do banco
