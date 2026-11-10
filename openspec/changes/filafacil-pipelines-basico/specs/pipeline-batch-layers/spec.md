# Spec Delta

## Purpose

Organiza os dados do pipeline batch da FilaFácil em camadas bruta, processada e consolidada, com particionamento por data e versionamento por execucao.

## ADDED Requirements

### Requirement: Camadas de dados obrigatorias
O sistema SHALL manter tres camadas de dados do pipeline: bruta (dados ingestados sem transformacao de negocio), processada (dados normalizados e limpos) e consolidada (modelo unificado e metricas).

#### Scenario: Estrutura de camadas apos execucao
- **WHEN** uma execucao do pipeline conclui com sucesso
- **THEN** existem saidas distintas nas camadas bruta, processada e consolidada

### Requirement: Particionamento por data
O sistema SHALL particionar as saidas do pipeline por data operacional (ou data de referencia da carga), de forma que particoes de datas diferentes nao se sobrescrevam.

#### Scenario: Duas datas em particoes separadas
- **WHEN** o pipeline processa registros com datas operacionais distintas
- **THEN** os artefatos consolidados ficam organizados em particoes separadas por data

### Requirement: Versionamento de execucao
O sistema SHALL registrar um identificador de versao/execucao para cada corrida do pipeline e associar as saidas a essa versao.

#### Scenario: Nova execucao gera nova versao
- **WHEN** o pipeline e executado novamente sobre o mesmo conjunto de fontes
- **THEN** a nova execucao recebe um identificador de versao distinto da anterior e as saidas ficam rastreaveis a essa versao
