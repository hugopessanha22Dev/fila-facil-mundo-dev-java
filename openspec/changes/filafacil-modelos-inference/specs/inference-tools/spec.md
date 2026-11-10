# Spec Delta

## Purpose

Define contratos de tools usadas na inferência operacional (entrada, saída e erro), permitindo simulação/mock sem perder clareza de integração com dados de coleta.

## ADDED Requirements

### Requirement: Tools com contrato explícito
O sistema MUST definir contrato de entrada, saída e erro para cada tool: consultar status de entrega; buscar dados de uma coleta; registrar ocorrência; calcular prioridade operacional; encaminhar para atendimento humano.

#### Scenario: Tool de busca de coleta
- **WHEN** a tool de buscar coleta é invocada com um identificador válido conhecido
- **THEN** retorna os dados da coleta conforme o contrato de saída

#### Scenario: Tool com identificador inexistente
- **WHEN** a tool de buscar coleta é invocada com identificador inexistente
- **THEN** retorna erro tipado conforme o contrato de erro, sem falha não tratada

### Requirement: Tools mockáveis
As tools MUST poder ser executadas em modo simulado (mock) com o mesmo contrato, para desenvolvimento e avaliação offline sem depender de efeitos colaterais reais.

#### Scenario: Avaliação offline com mocks
- **WHEN** a avaliação offline executa casos que usam tools
- **THEN** as tools mockadas respondem pelo contrato sem gravar ocorrência real nem chamar APIs externas não previstas
