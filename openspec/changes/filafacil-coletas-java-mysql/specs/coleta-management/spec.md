# Spec Delta

## Purpose

Define o comportamento do domínio de coletas da FilaFácil: persistência relacional, CRUD, validações e filtros estruturados que substituem o armazenamento em JSON.

## ADDED Requirements

### Requirement: Persistencia relacional de coletas
O sistema MUST persistir coletas em banco MySQL local chamado `fila-facil`, em uma unica tabela relacional com chave primaria, de forma que os dados sobrevivam entre execucoes da aplicacao.

#### Scenario: Dados sobrevivem a reinicio
- **WHEN** o usuario cadastra uma coleta e encerra a aplicacao
- **THEN** ao iniciar novamente a mesma coleta MUST estar disponivel na listagem

#### Scenario: Banco e tabela corretos
- **WHEN** a aplicacao inicia com MySQL configurado
- **THEN** as operacoes de coleta MUST usar o banco `fila-facil` e a tabela de coletas mapeada via ORM

### Requirement: Estrutura dos campos da coleta
Cada coleta MUST possuir identificador unico, nome do cliente, endereco, quantidade de pacotes, prioridade, status e data de criacao, com tipos coerentes com o uso de cada campo.

#### Scenario: Coleta completa apos cadastro
- **WHEN** uma coleta e cadastrada com sucesso
- **THEN** o registro MUST incluir id gerado, nome, endereco, pacotes, prioridade, status e data de criacao preenchidos

### Requirement: Cadastro com validacao de obrigatorios
O sistema MUST recusar cadastro quando campos obrigatorios estiverem vazios ou ausentes, e MUST persistir a coleta quando todos os obrigatorios forem validos.

#### Scenario: Cadastro valido
- **WHEN** o usuario informa nome, endereco, pacotes, prioridade e status validos
- **THEN** o sistema MUST inserir a coleta no banco e confirmar o sucesso

#### Scenario: Campo obrigatorio vazio
- **WHEN** o usuario tenta cadastrar com qualquer campo obrigatorio vazio
- **THEN** o sistema MUST rejeitar a operacao sem persistir o registro

### Requirement: Listagem ordenada de coletas
O sistema MUST listar todas as coletas ordenadas por identificador ou por data de criacao.

#### Scenario: Listar por id
- **WHEN** o usuario solicita a listagem ordenada por id
- **THEN** o sistema MUST exibir todas as coletas na ordem do identificador

#### Scenario: Listar por data
- **WHEN** o usuario solicita a listagem ordenada por data de criacao
- **THEN** o sistema MUST exibir todas as coletas na ordem da data de criacao

### Requirement: Filtro por status
O sistema MUST permitir buscar coletas filtrando pelo status, com condicao equivalente a WHERE no banco via ORM.

#### Scenario: Filtrar status existente
- **WHEN** o usuario filtra por um status que possui registros
- **THEN** o sistema MUST retornar apenas coletas com aquele status

#### Scenario: Filtrar status sem resultados
- **WHEN** o usuario filtra por um status sem coletas correspondentes
- **THEN** o sistema MUST retornar lista vazia sem erro

### Requirement: Filtro por prioridade
O sistema MUST permitir buscar coletas filtrando pela prioridade, com condicao equivalente a WHERE no banco via ORM.

#### Scenario: Filtrar prioridade existente
- **WHEN** o usuario filtra por uma prioridade que possui registros
- **THEN** o sistema MUST retornar apenas coletas com aquela prioridade

### Requirement: Filtro por nome do cliente
O sistema MUST permitir buscar coletas filtrando pelo nome do cliente, com condicao equivalente a WHERE no banco via ORM.

#### Scenario: Filtrar por nome correspondente
- **WHEN** o usuario informa um nome que corresponde a uma ou mais coletas
- **THEN** o sistema MUST retornar somente as coletas cujo nome atende ao filtro

### Requirement: Atualizacao de status por identificador
O sistema MUST atualizar o status de uma coleta existente pelo identificador unico e MUST informar falha quando o identificador nao existir.

#### Scenario: Atualizar status existente
- **WHEN** o usuario informa um id existente e um novo status valido
- **THEN** o sistema MUST persistir o novo status para aquela coleta

#### Scenario: Atualizar id inexistente
- **WHEN** o usuario tenta atualizar status de um id inexistente
- **THEN** o sistema MUST nao alterar o banco e MUST informar que a coleta nao foi encontrada

### Requirement: Exclusao com verificacao de existencia
O sistema MUST excluir uma coleta apenas apos confirmar que o identificador existe; se nao existir, MUST informar falha sem erro fatal.

#### Scenario: Deletar coleta existente
- **WHEN** o usuario confirma exclusao de um id existente
- **THEN** o sistema MUST remover a coleta do banco

#### Scenario: Deletar id inexistente
- **WHEN** o usuario tenta excluir um id inexistente
- **THEN** o sistema MUST informar que a coleta nao foi encontrada e MUST manter o banco inalterado
