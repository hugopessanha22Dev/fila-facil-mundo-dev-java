# Spec Delta

## Purpose

Centralizar a criacao de coletas validas em uma Factory, evitando montagem manual inconsistente de campos obrigatórios, status inicial e prioridade.

## ADDED Requirements

### Requirement: Criacao via Factory
A criacao de uma nova coleta MUST passar por uma Factory que valida dados obrigatorios e define status inicial e prioridade de forma consistente.

#### Scenario: Coleta valida criada pela Factory
- **WHEN** a Factory recebe dados obrigatorios validos
- **THEN** retorna uma coleta com status inicial padrao e prioridade definida

#### Scenario: Dados invalidos rejeitados na Factory
- **WHEN** a Factory recebe campo obrigatorio ausente ou invalido
- **THEN** rejeita a criacao sem persistir a coleta

### Requirement: Service nao monta coleta manualmente
O restante do sistema (incluindo o service) MUST NOT montar manualmente todos os campos de uma nova coleta fora da Factory.

#### Scenario: Cadastro usa Factory
- **WHEN** o usuario cadastra uma coleta
- **THEN** a entidade persistida foi produzida pela Factory
