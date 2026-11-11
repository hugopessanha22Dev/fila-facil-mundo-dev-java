# Spec Delta

## Purpose

Identificar cada prompt, dataset, embedding, schema e configuracao de modelo por versao, registrar qual combinacao rodou em cada build, avaliacao e deploy, e voltar para a combinacao anterior.

## ADDED Requirements

### Requirement: Artefatos de IA versionados
O sistema MUST versionar prompt, golden dataset, embedding, schema de saida e configuracao de modelo, sem gravar segredo dentro desses artefatos.

#### Scenario: Nova versao de prompt
- **WHEN** um prompt novo e publicado
- **THEN** ele recebe um identificador de versao proprio e a versao anterior continua recuperavel

#### Scenario: Artefato sem segredo
- **WHEN** um artefato versionado e lido
- **THEN** o conteudo nao contem chave de API nem dado real de cliente

### Requirement: Combinacao registrada por execucao
O sistema MUST gravar, em cada build, avaliacao e deploy, a combinacao de versoes de prompt, dataset, embedding, schema e configuracao de modelo.

#### Scenario: Avaliacao aponta as versoes
- **WHEN** uma avaliacao offline termina
- **THEN** o relatorio identifica a combinacao exata que foi medida

#### Scenario: Deploy aponta as versoes
- **WHEN** uma promocao e aceita
- **THEN** o ambiente de destino registra a mesma combinacao que foi avaliada

### Requirement: Rollback para a combinacao anterior
O sistema MUST conseguir recolocar prompt, configuracao, embedding ou aplicacao na combinacao que estava ativa antes da ultima promocao daquele ambiente.

#### Scenario: Rollback apos promocao
- **WHEN** o operador pede rollback em homologacao ou producao simulada
- **THEN** o ambiente volta a combinacao anterior e a combinacao recuada deixa de ser a ativa
