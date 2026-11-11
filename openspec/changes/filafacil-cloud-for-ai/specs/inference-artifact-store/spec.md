# Spec Delta

## Purpose

Organizar o que cada inferencia produz — datasets, prompts, avaliacoes, logs e resultados — por ambiente, data, versao e tipo, para reprocessar com seguranca e sem gravar dado sensivel.

## ADDED Requirements

### Requirement: Datasets separados por ambiente e estagio
O sistema MUST guardar datasets brutos e tratados em areas distintas, separadas por ambiente e dominio.

#### Scenario: Dataset bruto nao se mistura ao tratado
- **WHEN** um dataset bruto e ingerido
- **THEN** ele fica na area bruta do ambiente e o dataset tratado resultante fica na area tratada, com referencia a origem

### Requirement: Prompt versionado
O sistema MUST versionar cada prompt com identificador unico e registrar qual versao foi usada em cada inferencia.

#### Scenario: Inferencia aponta o prompt
- **WHEN** uma inferencia e concluida
- **THEN** o resultado referencia o identificador da versao de prompt utilizada

#### Scenario: Prompt novo nao reescreve o antigo
- **WHEN** uma nova versao de prompt e publicada
- **THEN** a versao anterior permanece recuperavel pelo identificador antigo

### Requirement: Artefatos de avaliacao
O sistema MUST armazenar artefatos de avaliacao organizados por versao de modelo, data e tipo de workload.

#### Scenario: Avaliacao arquivada
- **WHEN** uma avaliacao offline ou de ensaio e executada
- **THEN** o artefato fica enderecado por versao de modelo, data e tipo de workload

### Requirement: Resultado de inferencia com metadados
O sistema MUST persistir cada resultado com identificador, modelo, versao de prompt, latencia, tokens, custo estimado, status e, quando houver, o erro.

#### Scenario: Inferencia bem-sucedida
- **WHEN** o modelo devolve uma resposta aceita
- **THEN** o registro contem identificador, modelo, versao de prompt, latencia, tokens, custo estimado, status de sucesso e a resposta retornada

#### Scenario: Inferencia com erro
- **WHEN** a chamada falha por timeout, taxa ou erro do provedor
- **THEN** o registro contem status de erro e a descricao do erro, sem a chave de API

### Requirement: Nenhum dado sensivel armazenado
O sistema MUST NOT gravar chave de API, senha, documento pessoal ou dado real de cliente em prompt, log ou resultado.

#### Scenario: Tentativa de gravar segredo
- **WHEN** o texto de log ou de resultado contem um padrao de chave ou credencial
- **THEN** o sistema omite ou mascara o valor antes de persistir
