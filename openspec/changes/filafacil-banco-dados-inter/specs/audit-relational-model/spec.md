# Spec Delta

## Purpose

Definir o modelo relacional normalizado de auditoria de entregas e reembolsos com seeds suficientes para joins, filtros e paginacao.

## ADDED Requirements

### Requirement: Tabelas normalizadas com chaves
O sistema MUST persistir clientes, motoristas, entregas, ocorrencias e reembolsos em tabelas separadas relacionadas por chaves primarias e estrangeiras, sem repetir dados de cliente/motorista na entrega.

#### Scenario: Integridade referencial
- **WHEN** uma entrega e cadastrada/seedada
- **THEN** ela referencia cliente e motorista existentes via chave estrangeira

### Requirement: Seeds de dados de teste
O projeto MUST incluir seeds com volume suficiente para exercitar joins, filtros combinados e paginacao localmente.

#### Scenario: Seed carregavel
- **WHEN** o ambiente local aplica o schema e os seeds
- **THEN** existem registros em todas as tabelas do modelo suficientes para consultas de auditoria
