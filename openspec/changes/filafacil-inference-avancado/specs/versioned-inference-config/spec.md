# Spec Delta

## Purpose

Versionar os parâmetros de inferência de cada tarefa e permitir voltar a versão anterior quando a qualidade cair.

## ADDED Requirements

### Requirement: Parametros por tarefa
O sistema MUST guardar, por tarefa e por versão, temperature, top-p, max tokens, stop sequences e seed, e MUST ligar streaming só em resumo e resposta assistida.

#### Scenario: Extracao sem streaming
- **WHEN** a configuração ativa da extração é lida
- **THEN** streaming está desligado e a temperature é menor ou igual à da classificação

### Requirement: Versao ativa e rollback
O sistema MUST apontar uma versão ativa e MUST conseguir promover outra versão e voltar à anterior sem apagar o histórico.

#### Scenario: Rollback
- **WHEN** a versão v2 está ativa e o rollback é pedido
- **THEN** a versão ativa volta a ser a anterior e a v2 continua registrada
