# Spec Delta

## Purpose

Produz um relatorio de validacao do manifesto com totais, lista de problemas e status final aprovado ou reprovado, exibivel no terminal e/ou salvo em arquivo.

## ADDED Requirements

### Requirement: Totais e lista de erros
O relatorio MUST informar total de registros analisados, total de erros e a lista detalhada dos problemas encontrados.

#### Scenario: Manifesto com erros
- **WHEN** a validacao encontra um ou mais problemas
- **THEN** o relatorio lista cada erro e mostra totais de registros e erros

### Requirement: Status final
O relatorio MUST concluir com status `aprovado` quando nao houver erros e `reprovado` quando houver ao menos um erro.

#### Scenario: Manifesto limpo
- **WHEN** todos os registros passam nas regras
- **THEN** o status final do relatorio e aprovado

### Requirement: Saida no terminal ou arquivo
O sistema MUST exibir o relatorio no terminal e MUST permitir salvar o resultado em arquivo local.

#### Scenario: Salvar relatorio
- **WHEN** o operador solicita persistir o relatorio
- **THEN** um arquivo local e gravado com o conteudo do relatorio
