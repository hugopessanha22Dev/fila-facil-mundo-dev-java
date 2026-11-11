# Spec Delta

## Purpose

Deixar um rastro auditavel de cada inferencia da esteira, com versao, modelo, latencia, custo, tokens, retries e fallback, sem chave e sem dado sensivel.

## ADDED Requirements

### Requirement: Trace de execucao
O sistema MUST registrar, para cada caso avaliado, o identificador, a versao do prompt, o modelo, a configuracao, a latencia, o custo estimado, os tokens, o numero de retries, se houve fallback e o erro quando existir.

#### Scenario: Caso concluido
- **WHEN** um caso do golden dataset e inferido com sucesso
- **THEN** o trace contem versao do prompt, modelo, latencia, custo, tokens e retries, e marca fallback como falso

#### Scenario: Fallback acionado
- **WHEN** a saida primaria falha o schema e o modelo de fallback responde
- **THEN** o trace marca fallback como verdadeiro e conserva o erro da tentativa primaria

### Requirement: Trace sem dado sensivel
O sistema MUST mascarar chave de API e dado pessoal antes de gravar prompt, resposta ou erro no trace.

#### Scenario: Segredo no texto
- **WHEN** o texto do caso ou da resposta contem uma chave de API
- **THEN** o trace gravado substitui a chave por mascara e nao contem o valor original
