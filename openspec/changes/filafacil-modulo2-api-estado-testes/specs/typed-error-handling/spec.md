# Spec Delta

## Purpose

Define o tratamento tipado de erros de entrada, arquivo e API no FilaFacil intermediario, com mensagens claras e continuidade do loop de terminal.

## ADDED Requirements

### Requirement: Classificar erros por origem
O sistema MUST distinguir erros de entrada do usuario, de persistencia em arquivo e de integracao com API REST.

#### Scenario: Erro de entrada invalida
- **WHEN** o usuario informa campo obrigatorio vazio ou valor fora do dominio
- **THEN** o sistema MUST classificar como erro de entrada e exibir mensagem especifica sem encerrar

#### Scenario: Erro de arquivo
- **WHEN** a leitura ou escrita do JSON falha
- **THEN** o sistema MUST classificar como erro de arquivo, registrar no estado e continuar operacional quando possivel

#### Scenario: Erro de API
- **WHEN** a chamada REST falha
- **THEN** o sistema MUST classificar como erro de API e expor mensagem clara no terminal

### Requirement: Ultimo erro visivel no estado
O ultimo erro tratado MUST ficar disponivel no estado central da aplicacao para consulta/exibicao.

#### Scenario: Consultar ultimo erro apos falha
- **WHEN** ocorre uma falha tratada de qualquer origem
- **THEN** o estado MUST armazenar o ultimo erro com origem e mensagem
