# Spec Delta

## Purpose

Persiste checkpoints isolados por fonte e garante que reexecucao e reprocessamento sejam idempotentes sem duplicar resultados.

## ADDED Requirements

### Requirement: Checkpoint por fonte
O sistema SHALL registrar, por fonte, o ultimo ponto processado: data/intervalo, status, lote e quantidade de registros, sem misturar estados entre fontes.

#### Scenario: Duas fontes com checkpoints distintos
- **WHEN** coletas e entregas sao processadas na mesma execucao
- **THEN** cada fonte possui checkpoint proprio com intervalo e volume independentes

### Requirement: Atualizacao de checkpoint apos sucesso
O sistema SHALL atualizar o checkpoint de uma fonte somente apos a carga daquela fonte concluir com sucesso na execucao.

#### Scenario: Falha nao avanca checkpoint
- **WHEN** o processamento de uma fonte falha
- **THEN** o checkpoint dessa fonte permanece no valor anterior

### Requirement: Idempotencia de reexecucao
O sistema SHALL permitir repetir a mesma execucao (mesmo intervalo/fonte) sem gerar duplicidades na camada final.

#### Scenario: Segunda execucao do mesmo lote
- **WHEN** o mesmo intervalo incremental e reprocessado com sucesso
- **THEN** a camada final nao duplica chaves de negocio ja consolidadas
