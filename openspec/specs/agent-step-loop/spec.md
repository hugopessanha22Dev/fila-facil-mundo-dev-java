# agent-step-loop Specification

## Purpose

Fazer o agente interpretar, planejar e executar uma ferramenta por passo, observando o retorno antes de decidir o próximo, com estado e trace completos.

## Requirements

### Requirement: Um ferramenta por passo
O sistema MUST executar no máximo uma ferramenta por passo e MUST observar o retorno antes de escolher a próxima ação.

#### Scenario: Consulta antes do registro
- **WHEN** o caso tem código e pede registro de ocorrência
- **THEN** o primeiro passo só consulta, e o registro acontece num passo posterior, depois do retorno da consulta

### Requirement: Estado e trace
O sistema MUST atualizar o estado do caso a cada observação e MUST gravar, em cada passo, a interpretação, a ferramenta, os argumentos, o resultado e a decisão seguinte.

#### Scenario: Trace do fluxo
- **WHEN** um caso termina
- **THEN** o trace lista os passos na ordem, com ferramenta e resultado de cada um

### Requirement: Sem dado inventado
O sistema MUST preencher status, cliente e próxima etapa só com valor devolvido por ferramenta ou informado na mensagem, e MUST marcar ausente o que não veio de nenhuma das duas fontes.

#### Scenario: Ferramenta sem cliente
- **WHEN** a consulta não devolve cliente
- **THEN** a saída final não inventa nome de cliente e marca o campo como ausente
