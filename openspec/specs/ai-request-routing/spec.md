# ai-request-routing Specification

## Purpose

Encaminhar cada pedido operacional a um único fluxo já existente, declarando a entrada, a saída, o risco e a validação exigida.

## Requirements

### Requirement: Seis fluxos reconhecidos
O sistema MUST classificar o pedido como atendimento, triagem, busca, agente, classificador ou resumo, e MUST declarar a entrada, a saída esperada, o risco e a validação do fluxo escolhido.

#### Scenario: Atendimento com evidencia
- **WHEN** a mensagem é de um cliente sobre uma regra operacional
- **THEN** o fluxo escolhido é atendimento e a validação exigida é resposta com evidência

#### Scenario: Triagem de ocorrencia
- **WHEN** a mensagem pede a triagem de uma ocorrência
- **THEN** o fluxo escolhido é triagem e a saída esperada traz categoria e prioridade

### Requirement: Um fluxo por pedido
O sistema MUST escolher um único fluxo por pedido e MUST recusar pedido fora desses fluxos sem chamar modelo nem ferramenta.

#### Scenario: Pedido fora de escopo
- **WHEN** a mensagem não descreve uma tarefa logística
- **THEN** nenhum fluxo é executado e o resultado registra fora de escopo
