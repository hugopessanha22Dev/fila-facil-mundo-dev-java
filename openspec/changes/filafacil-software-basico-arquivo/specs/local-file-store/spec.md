# Spec Delta

## Purpose

Define a persistencia local em arquivo unico para as coletas, substituindo banco de dados: carregar na abertura e gravar apos cadastro ou alteracao.

## ADDED Requirements

### Requirement: Persistencia apenas em arquivo local
O sistema MUST armazenar as coletas em um unico arquivo local (`.json` ou `.txt`) e MUST NOT depender de banco de dados, API remota ou framework de persistencia.

#### Scenario: Sem dependencia de banco
- **WHEN** o sistema inicia e opera coletas
- **THEN** leitura e escrita MUST ocorrer somente via arquivo local do projeto

### Requirement: Carregar dados ao iniciar
O sistema MUST ler o arquivo de coletas ao iniciar e disponibilizar os registros em memoria para as operacoes do menu.

#### Scenario: Arquivo existente com dados
- **WHEN** o arquivo local ja contem coletas e a aplicacao inicia
- **THEN** a listagem MUST refletir os dados salvos anteriormente

### Requirement: Gravar apos cadastro ou alteracao
O sistema MUST escrever o arquivo local apos um cadastro bem-sucedido ou apos uma atualizacao de status bem-sucedida.

#### Scenario: Persistencia apos cadastro
- **WHEN** uma coleta e cadastrada com sucesso
- **THEN** o arquivo local MUST ser atualizado com o novo registro

#### Scenario: Persistencia apos atualizacao
- **WHEN** o status de uma coleta e atualizado com sucesso
- **THEN** o arquivo local MUST refletir o novo status

### Requirement: Primeira execucao sem arquivo
O sistema MUST tratar o caso em que o arquivo ainda nao existe na primeira execucao, iniciando com lista vazia sem encerrar o programa.

#### Scenario: Arquivo ausente no boot
- **WHEN** a aplicacao inicia e o arquivo de coletas nao existe
- **THEN** o sistema MUST continuar normalmente com nenhuma coleta carregada
