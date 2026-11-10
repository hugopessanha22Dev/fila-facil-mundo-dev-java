# Spec Delta

## Purpose

Fornece lineage de origem ate destino e observabilidade operacional com metricas, alertas e auditoria por execucao do pipeline intermediario.

## ADDED Requirements

### Requirement: Lineage por registro ou lote
O sistema SHALL registrar origem, transformacoes aplicadas e destino suficientes para responder de onde veio um registro, quando foi processado e qual execucao o alterou.

#### Scenario: Consulta de origem de um registro consolidado
- **WHEN** um registro consolidado e inspecionado quanto a lineage
- **THEN** constam fonte de origem, runId da execucao e destino da camada final

### Requirement: Auditoria por execucao
O sistema SHALL registrar por execucao: fontes processadas, intervalo considerado, checkpoint atualizado, volumes (lido, descartado, inserido, atualizado, deduplicado, atrasado) e status final.

#### Scenario: Relatorio de execucao bem-sucedida
- **WHEN** a execucao termina com sucesso
- **THEN** o relatorio/auditoria inclui fontes, intervalo, checkpoints, volumes e status SUCCESS

### Requirement: Metricas e alertas operacionais
O sistema SHALL expor tempo por etapa e emitir alerta observavel (log/terminal) para falha de etapa ou volume inesperado (zero lidos quando esperado > 0, ou descartados acima de limiar configuravel).

#### Scenario: Alerta de falha de etapa
- **WHEN** uma etapa falha
- **THEN** um alerta de falha e registrado com a etapa e o motivo

#### Scenario: Alerta de volume inesperado
- **WHEN** uma fonte obrigatoria retorna zero registros lidos
- **THEN** um alerta de volume inesperado e registrado para essa fonte
