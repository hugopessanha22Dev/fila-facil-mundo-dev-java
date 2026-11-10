# Spec Delta

## Purpose

Integra pelo menos quatro fontes operacionais com formatos e frequencias distintas, com particionamento colunar e paralelismo controlado no pipeline intermediario.

## ADDED Requirements

### Requirement: Quatro fontes integradas
O sistema SHALL consumir ao menos quatro fontes: API de coletas (simulada), banco relacional de clientes, arquivos CSV/Parquet de entregas e JSON de motoristas/rotas (SaaS simulado).

#### Scenario: Execucao consome as quatro fontes
- **WHEN** o pipeline intermediario executa com fixtures disponiveis
- **THEN** as quatro fontes sao processadas e aparecem no relatorio de execucao

### Requirement: Parquet na camada processada
O sistema SHALL gravar a camada processada em formato Parquet particionado (por data e ao menos um de: regiao, status ou dominio).

#### Scenario: Particao processada em Parquet
- **WHEN** a etapa de transformacao/carga processada conclui
- **THEN** existem arquivos Parquet sob particoes nomeadas (ex.: dt=.../regiao=...)

### Requirement: Paralelismo controlado
O sistema SHALL processar fontes (ou particoes) com paralelismo controlado e rastreavel, sem perder a correlacao de cada resultado a sua fonte.

#### Scenario: Fontes processadas em paralelo
- **WHEN** o modo inter executa com paralelismo habilitado
- **THEN** mais de uma fonte pode ser processada concorrentemente e cada uma registra seu proprio volume/status no log da execucao

### Requirement: Disparo pelo terminal
O sistema SHALL permitir disparar o pipeline intermediario via menu ou CLI, preservando as opcoes de menu existentes (0–11).

#### Scenario: Nova opcao ou flag nao remove menus
- **WHEN** o modo inter e adicionado ao terminal
- **THEN** as opcoes anteriores do menu permanecem acessiveis
