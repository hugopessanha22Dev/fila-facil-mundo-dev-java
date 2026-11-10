# Spec Delta

## Purpose

Ingesta e valida as fontes operacionais diarias da FilaFácil em CSV, JSON, Parquet e tabela relacional antes de qualquer transformacao.

## ADDED Requirements

### Requirement: Ingestao CSV de coletas
O sistema SHALL ler um arquivo CSV de coletas realizadas contendo ao menos codigo da coleta, cliente, regiao, responsavel, status, prazo previsto, prazo realizado e data da operacao.

#### Scenario: CSV valido e carregado
- **WHEN** o arquivo CSV de coletas existe e possui cabecalho e registros validos
- **THEN** os registros sao ingestados na camada bruta com contagem de lidos maior que zero

### Requirement: Ingestao JSON de ocorrencias
O sistema SHALL ler um arquivo JSON de ocorrencias contendo tipo, descricao, referencia a coleta ou entrega, data, responsavel e status de resolucao.

#### Scenario: JSON valido e carregado
- **WHEN** o arquivo JSON de ocorrencias existe e e parseavel
- **THEN** as ocorrencias sao ingestadas na camada bruta

### Requirement: Ingestao Parquet de entregas
O sistema SHALL ler um arquivo Parquet de entregas concluidas contendo identificador, regiao, data de conclusao, prazo original, status final, indicador de atraso, cliente e unidade.

#### Scenario: Parquet valido e carregado
- **WHEN** o arquivo Parquet de entregas existe e e legivel
- **THEN** as entregas sao ingestadas na camada bruta

### Requirement: Ingestao de tabela relacional de clientes e unidades
O sistema SHALL ler a tabela relacional de clientes/unidades (cadastro com identificador, regiao e unidade) para uso em joins e enriquecimento.

#### Scenario: Cadastro relacional disponivel
- **WHEN** a tabela de clientes/unidades esta seedada e acessivel
- **THEN** o pipeline carrega o cadastro e o disponibiliza para a etapa de transformacao

### Requirement: Validacao antes da transformacao
O sistema SHALL validar existencia e formato basico de cada fonte obrigatoria antes de iniciar transformacao; falha de fonte obrigatoria MUST abortar a etapa de ingestao com erro explicito.

#### Scenario: Fonte obrigatoria ausente
- **WHEN** uma fonte obrigatoria (CSV, JSON, Parquet ou tabela) esta ausente ou ilegivel
- **THEN** a ingestao falha com mensagem indicando a fonte e nenhuma transformacao de negocio e aplicada
