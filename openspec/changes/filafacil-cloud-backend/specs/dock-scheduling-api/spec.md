# Spec Delta

## Purpose

Define a API HTTP de agendamento de docas para transportadoras reservarem horario com transportadora, placa e doca, evitando conflitos manuais.

## ADDED Requirements

### Requirement: Criar agendamento
A API MUST permitir criar um agendamento informando transportadora, placa e doca (e data/horario quando aplicavel), persistindo o registro com status inicial valido.

#### Scenario: Criacao bem-sucedida
- **WHEN** um cliente envia uma requisicao valida de criacao de agendamento
- **THEN** a API responde com sucesso e retorna o agendamento criado com identificador

#### Scenario: Campos obrigatorios ausentes
- **WHEN** a requisicao de criacao omite transportadora, placa ou doca
- **THEN** a API rejeita a operacao com erro de validacao sem persistir o registro

### Requirement: Listar agendamentos
A API MUST listar todos os agendamentos cadastrados.

#### Scenario: Lista com registros
- **WHEN** existem agendamentos persistidos e o cliente solicita a listagem
- **THEN** a API retorna a colecao completa dos agendamentos

### Requirement: Consultar por data
A API MUST permitir consultar agendamentos filtrados por uma data especifica.

#### Scenario: Filtro por data
- **WHEN** o cliente solicita agendamentos de uma data valida
- **THEN** a API retorna apenas os agendamentos daquela data

### Requirement: Atualizar status
A API MUST permitir atualizar o status de um agendamento existente.

#### Scenario: Status atualizado
- **WHEN** o cliente envia um novo status valido para um agendamento existente
- **THEN** a API persiste o novo status e o reflete nas consultas seguintes

### Requirement: Cancelar agendamento
A API MUST permitir cancelar um agendamento existente, refletindo o cancelamento no status ou marca equivalente.

#### Scenario: Cancelamento
- **WHEN** o cliente solicita o cancelamento de um agendamento existente
- **THEN** a API marca o agendamento como cancelado e nao o trata mais como ativo
