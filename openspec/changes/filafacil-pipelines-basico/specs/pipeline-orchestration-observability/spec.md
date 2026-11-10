# Spec Delta

## Purpose

Orquestra as etapas do pipeline batch em ordem, aplica retries em falhas transitivas e registra logs de execucao uteis para investigacao.

## ADDED Requirements

### Requirement: Ordem de etapas com dependencias
O sistema SHALL executar as etapas na ordem ingestao/validacao → transformacao/normalizacao → carga/organizacao, sem iniciar uma etapa dependente se a anterior falhou.

#### Scenario: Transformacao bloqueada apos falha de ingestao
- **WHEN** a ingestao falha
- **THEN** transformacao e carga nao sao executadas e o status final da execucao e de falha

### Requirement: Retries em falhas transitivas
O sistema SHALL permitir retries configuraveis em pontos de falha transitiva e MUST NAO mascarar erros permanentes apos esgotar as tentativas.

#### Scenario: Falha permanente apos retries
- **WHEN** uma etapa falha de forma permanente apos o numero maximo de retries
- **THEN** o pipeline aborta a etapa com o erro original e registra a falha nos logs

### Requirement: Logs de execucao rastreaveis
O sistema SHALL registrar para cada execucao: inicio, fim, tempo por etapa, quantidade de registros lidos, descartados e gravados, e status final.

#### Scenario: Log completo em sucesso
- **WHEN** o pipeline conclui com sucesso
- **THEN** o log da execucao inclui horario de inicio/fim, duracao por etapa, contagens de lidos/descartados/gravados e status de sucesso

#### Scenario: Log de falha nao omite etapa
- **WHEN** uma etapa falha
- **THEN** o log identifica a etapa, o motivo e o status de falha da execucao
