# Spec Delta

## Purpose

Define o ciclo operacional das coletas no case de software basico: cadastro validado, listagem, filtros por status/prioridade e atualizacao de status no terminal.

## ADDED Requirements

### Requirement: Cadastrar coleta com status inicial pendente
O sistema MUST cadastrar uma coleta com nome do cliente, endereco, quantidade de pacotes e prioridade (`baixa`, `media` ou `alta`), definindo o status inicial sempre como `pendente` e gerando um identificador numerico local unico.

#### Scenario: Cadastro valido
- **WHEN** o usuario informa nome, endereco, pacotes (> 0) e prioridade valida
- **THEN** o sistema MUST criar a coleta com status `pendente` e um id numerico unico

#### Scenario: Campo obrigatorio vazio
- **WHEN** o usuario tenta cadastrar com nome, endereco, pacotes ou prioridade vazios/ausentes
- **THEN** o sistema MUST rejeitar o cadastro sem adicionar o registro

### Requirement: Listar todas as coletas com identificador
O sistema MUST listar todas as solicitacoes exibindo ao menos o identificador e os dados principais de cada coleta.

#### Scenario: Listagem com registros
- **WHEN** existem coletas cadastradas e o usuario solicita listagem
- **THEN** o sistema MUST exibir todas as coletas com seus identificadores

#### Scenario: Listagem vazia
- **WHEN** nao ha coletas e o usuario solicita listagem
- **THEN** o sistema MUST informar que nao ha registros, sem erro fatal

### Requirement: Filtrar por status
O sistema MUST filtrar coletas por status `pendente`, `em_rota` ou `concluida` e MUST exibir somente os registros encontrados.

#### Scenario: Filtro com resultados
- **WHEN** o usuario filtra por um status que possui coletas
- **THEN** o sistema MUST retornar apenas coletas com aquele status

#### Scenario: Filtro sem resultados
- **WHEN** o usuario filtra por um status sem coletas correspondentes
- **THEN** o sistema MUST indicar que nenhum registro foi encontrado

### Requirement: Filtrar por prioridade
O sistema MUST filtrar coletas por prioridade `baixa`, `media` ou `alta` e MUST exibir somente os registros encontrados.

#### Scenario: Filtro por prioridade com resultados
- **WHEN** o usuario filtra por uma prioridade que possui coletas
- **THEN** o sistema MUST retornar apenas coletas com aquela prioridade

### Requirement: Atualizar status de coleta especifica
O sistema MUST alterar o status de uma coleta existente pelo identificador para um dos valores `pendente`, `em_rota` ou `concluida`, e MUST informar falha se o id nao existir.

#### Scenario: Atualizacao com id existente
- **WHEN** o usuario informa um id existente e um status valido
- **THEN** o sistema MUST persistir o novo status naquela coleta

#### Scenario: Atualizacao com id inexistente
- **WHEN** o usuario informa um id inexistente
- **THEN** o sistema MUST informar que a coleta nao foi encontrada e MUST nao alterar outros registros
