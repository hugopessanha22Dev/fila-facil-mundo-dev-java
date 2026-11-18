# async-ai-work Specification

## Purpose

Tirar embedding, reprocessamento de documento e análise em lote do caminho síncrono, executando-os numa fila local com retry, timeout e evento de status.

## Requirements

### Requirement: Fila separada da resposta
O sistema MUST aceitar embedding, reprocessamento e análise em lote numa fila e MUST devolver o aceite sem esperar o processamento terminar.

#### Scenario: Aceite imediato
- **WHEN** o pedido pede reprocessamento de documento
- **THEN** a resposta síncrona traz o identificador do trabalho e o status aceito

### Requirement: Retry e timeout do worker
O sistema MUST executar o trabalho no worker, MUST repetir uma falha temporária até o teto e MUST encerrar por timeout quando a dependência passar do limite.

#### Scenario: Falha temporaria
- **WHEN** a primeira execução falha de forma temporária e a seguinte conclui
- **THEN** o status final é concluído e o evento registra a tentativa extra

#### Scenario: Tempo esgotado
- **WHEN** a dependência passa do tempo limite
- **THEN** o trabalho termina em falha por timeout e não permanece processando

### Requirement: Evento de status
O sistema MUST registrar cada mudança entre aceito, processando, concluído e falhou.

#### Scenario: Trilha concluida
- **WHEN** o worker conclui um embedding
- **THEN** a trilha contém aceito, processando e concluído, nessa ordem
