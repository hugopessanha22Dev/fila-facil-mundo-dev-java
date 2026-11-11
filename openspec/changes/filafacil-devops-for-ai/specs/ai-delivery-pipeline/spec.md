# Spec Delta

## Purpose

Levar uma mudanca de triagem ate homologacao ou producao simulada somente depois de lint, testes, prompt, schema e avaliacao offline, e barrar a release quando um gate minimo falha.

## ADDED Requirements

### Requirement: Esteira executa as etapas de qualidade
O sistema MUST executar, em ordem, validacao de prompt, testes da triagem, checagem de schema de saida e avaliacao offline antes de oferecer promocao.

#### Scenario: Esteira completa
- **WHEN** o operador dispara a esteira sobre uma combinacao de versoes
- **THEN** o relatorio lista cada etapa com aprovado ou reprovado e so oferece promocao se todas as obrigatorias passaram

### Requirement: Release bloqueada sem avaliacao aprovada
O sistema MUST recusar a promocao para o ambiente final quando a avaliacao offline nao tiver sido aprovada para aquela combinacao de versoes.

#### Scenario: Promocao sem avaliacao
- **WHEN** o operador pede promocao de uma combinacao que nao tem avaliacao offline aprovada
- **THEN** o ambiente de destino permanece na versao anterior e o comando termina com falha

#### Scenario: Promocao apos avaliacao aprovada
- **WHEN** a avaliacao offline da combinacao foi aprovada e o operador promove para homologacao ou producao simulada
- **THEN** o ambiente de destino passa a apontar para essa combinacao

### Requirement: Build so depois dos gates
O sistema MUST empacotar a release apenas quando os gates obrigatorios estiverem aprovados.

#### Scenario: Gate reprovado impede o pacote
- **WHEN** a validacao de prompt, o schema ou a avaliacao offline reprova
- **THEN** nenhum pacote de release e publicado para o ambiente de destino
