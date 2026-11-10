# Spec Delta

## Purpose

Permitir priorizacao de coletas por estrategias intercambiaveis (Strategy), sem alterar o service ao trocar a regra.

## ADDED Requirements

### Requirement: Contrato de estrategia de priorizacao
O sistema MUST expor um contrato de estrategia de priorizacao usado pelo service de coletas.

#### Scenario: Service usa o contrato
- **WHEN** uma coleta e criada ou sua prioridade e calculada/atribuida
- **THEN** o service consulta a estrategia injetada, nao uma regra hardcoded unica

### Requirement: Duas ou mais estrategias concretas
O sistema MUST fornecer pelo menos duas implementacoes concretas de estrategia de priorizacao (por exemplo: por quantidade de pacotes e por prioridade manual/informada).

#### Scenario: Troca de estrategia sem mudar o service
- **WHEN** a aplicacao injeta uma estrategia diferente no service
- **THEN** o resultado de priorizacao muda sem alterar o codigo do service

### Requirement: Estrategias testaveis isoladamente
Cada estrategia MUST poder ser testada de forma isolada com entradas determinísticas.

#### Scenario: Teste de estrategia por pacotes
- **WHEN** a estrategia baseada em quantidade de pacotes recebe entradas conhecidas
- **THEN** retorna a prioridade esperada para cada faixa
