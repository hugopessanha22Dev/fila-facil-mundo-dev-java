# Spec Delta

## Purpose

Notificar observadores registrados quando coletas sao criadas ou tem status atualizado, sem espalhar efeitos colaterais pelo service.

## ADDED Requirements

### Requirement: Eventos de criacao e atualizacao de status
O sistema MUST notificar observadores registrados apos criacao de coleta e apos atualizacao de status.

#### Scenario: Observador na criacao
- **WHEN** uma coleta e criada com sucesso
- **THEN** todos os observadores registrados recebem o evento de criacao

#### Scenario: Observador na atualizacao de status
- **WHEN** o status de uma coleta existente e atualizado com sucesso
- **THEN** todos os observadores registrados recebem o evento de atualizacao de status

### Requirement: Multiplos observadores concretos
O sistema MUST incluir pelo menos dois observadores concretos (por exemplo: log operacional e contador em memoria), sem mensageria real.

#### Scenario: Contador incrementa
- **WHEN** ocorre um evento de coleta observado pelo contador em memoria
- **THEN** o contador reflete o incremento correspondente

### Requirement: Service nao espalha efeitos secundarios
O service MUST acionar observadores via contrato/registro, sem implementar detalhes de log/notificacao inline misturados a regra principal.

#### Scenario: Observador falso em teste
- **WHEN** um observador fake e injetado/registrado no service
- **THEN** o teste consegue verificar que o evento foi recebido sem depender de I/O real
