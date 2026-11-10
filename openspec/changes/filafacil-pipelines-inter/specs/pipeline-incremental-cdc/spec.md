# Spec Delta

## Purpose

Permite carga batch completa e incremental com CDC simulado por campo de controle, evitando recarregar todo o historico a cada execucao.

## ADDED Requirements

### Requirement: Modo batch e incremental
O sistema SHALL oferecer execucao em modo batch (carga completa) e modo incremental (apenas registros novos ou alterados desde o ultimo checkpoint da fonte).

#### Scenario: Incremental processa apenas delta
- **WHEN** o pipeline roda em modo incremental apos uma carga previa
- **THEN** apenas registros com campo de controle posterior ao checkpoint da fonte sao lidos

#### Scenario: Batch recarrega historico
- **WHEN** o pipeline roda em modo batch
- **THEN** a fonte e consumida por completo independentemente do checkpoint

### Requirement: CDC simulado por campo de controle
O sistema SHALL classificar mudancas logicas (insert, update, delete) com base em campo de controle (timestamp de atualizacao, versao ou marcador CDC) por registro.

#### Scenario: Update nao e tratado como insert cego
- **WHEN** um registro existente chega com versao/timestamp mais recente
- **THEN** a mudanca e classificada como update (nao como insert duplicado)

#### Scenario: Delete logico
- **WHEN** um registro traz marcador de delete logico
- **THEN** a camada final reflete a exclusao logica sem recriar o registro ativo
