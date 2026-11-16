# prompt-example-coverage Specification

## Purpose

Dar a cada fluxo exemplos positivos suficientes e um contraexemplo que mostre a resposta que o sistema deve recusar.

## Requirements

### Requirement: Exemplos positivos por fluxo
O sistema MUST registrar pelo menos três exemplos positivos para cada fluxo de prompt, com entrada e saída esperada, sem dados reais de cliente.

#### Scenario: Tres exemplos
- **WHEN** o catálogo de um fluxo é lido
- **THEN** esse fluxo tem ao menos três exemplos do tipo positivo

### Requirement: Contraexemplo por prompt
O sistema MUST registrar ao menos um contraexemplo em cada prompt, mostrando uma resposta que deve ser rejeitada.

#### Scenario: Contraexemplo presente
- **WHEN** o catálogo de um prompt é lido
- **THEN** existe ao menos um exemplo do tipo contraexemplo para esse prompt
