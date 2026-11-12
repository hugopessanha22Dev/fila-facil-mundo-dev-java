# Spec Delta

## Purpose

Representar a exceção de entrega como entidade com identidade, valor e regra de ciclo de vida, sem depender de banco, API ou menu.

## ADDED Requirements

### Requirement: Identidade da excecao
O sistema MUST criar uma exceção com identificador próprio, código da entrega, tipo, origem, severidade, status inicial aberta, descrição, data de abertura e janela de resolução.

#### Scenario: Abertura completa
- **WHEN** a abertura traz entrega, tipo, origem, severidade, descrição e janela
- **THEN** a exceção nasce aberta, com identificador novo e a data de abertura preenchida

#### Scenario: Dado obrigatorio ausente
- **WHEN** a abertura chega sem código da entrega ou sem descrição
- **THEN** nenhuma exceção é criada

### Requirement: Transicoes validas
O sistema MUST permitir analisar só a partir de aberta, escalar só a partir de em análise quando a severidade for alta ou crítica, resolver só a partir de em análise ou escalada, e cancelar só a partir de aberta ou em análise.

#### Scenario: Resolucao sem analise
- **WHEN** a exceção ainda está aberta e recebe pedido de resolução
- **THEN** o status permanece aberta e a operação é recusada

#### Scenario: Severidade critica
- **WHEN** a exceção em análise tem severidade crítica e recebe resolução direta
- **THEN** a resolução é recusada até que a exceção seja escalada

### Requirement: Evento a cada mudanca
O sistema MUST emitir um evento de domínio com o identificador da exceção, o status anterior e o status novo em cada transição aceita.

#### Scenario: Analise registrada
- **WHEN** uma exceção aberta passa para em análise
- **THEN** o evento informa a passagem de aberta para em análise
