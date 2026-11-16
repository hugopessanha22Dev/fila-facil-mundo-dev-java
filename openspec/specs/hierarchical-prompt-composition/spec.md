# hierarchical-prompt-composition Specification

## Purpose

Separar instrução global, regra da tarefa e formato de saída, e impedir que o texto do operador se misture com essas regras.

## Requirements

### Requirement: Tres camadas de instrucao
O sistema MUST montar o prompt com três camadas distintas: regras globais, regras da tarefa e regras de formato.

#### Scenario: Prompt composto
- **WHEN** um fluxo pede o prompt de uma tarefa
- **THEN** a saída contém as três camadas em blocos separados

### Requirement: Entrada delimitada
O sistema MUST colocar o texto do operador entre delimitadores e MUST manter esse texto fora das camadas de instrução.

#### Scenario: Texto com aparencia de instrucao
- **WHEN** a mensagem do operador contém uma frase no imperativo
- **THEN** essa frase aparece só dentro dos delimitadores da entrada
