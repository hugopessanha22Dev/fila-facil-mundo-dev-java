# Spec Delta

## Purpose

Define a experiencia de terminal da FilaFacil: menu interativo em loop e separacao de camadas para que o usuario opere coletas sem acessar o banco diretamente pela interface.

## ADDED Requirements

### Requirement: Menu interativo em loop
A aplicacao MUST apresentar um menu interativo no terminal em loop ate o usuario escolher sair, expondo as operacoes de coletas previstas pelo dominio.

#### Scenario: Exibir opcoes principais
- **WHEN** a aplicacao inicia com sucesso
- **THEN** o usuario MUST ver um menu com opcoes de cadastrar, listar, filtrar, atualizar status, deletar e sair

#### Scenario: Continuar apos operacao
- **WHEN** o usuario conclui uma operacao do menu (exceto sair)
- **THEN** o sistema MUST retornar ao menu para nova escolha

#### Scenario: Encerrar aplicacao
- **WHEN** o usuario escolhe a opcao de sair
- **THEN** o loop do menu MUST encerrar a aplicacao de forma controlada

### Requirement: Separacao controller service repository
A camada de controller/menu MUST validar a entrada do usuario e delegar ao service; o service MUST aplicar regras e validacoes de negocio; o repository MUST concentrar 100% da interacao com o banco via ORM.

#### Scenario: Controller sem acesso a banco
- **WHEN** o usuario dispara qualquer operacao pelo menu
- **THEN** o controller MUST nao executar insert, select, update ou delete diretamente no banco

#### Scenario: Repository unico ponto de persistencia
- **WHEN** uma operacao precisa ler ou gravar coletas
- **THEN** apenas o repository MUST realizar a comunicacao com o ORM/banco

### Requirement: Feedback de operacoes no terminal
O sistema MUST exibir no terminal o resultado das operacoes (sucesso, rejeicao de validacao, registro nao encontrado ou lista/filtro), de forma legivel ao usuario.

#### Scenario: Feedback de sucesso no cadastro
- **WHEN** um cadastro e aceito e persistido
- **THEN** o terminal MUST informar sucesso ao usuario

#### Scenario: Feedback de validacao
- **WHEN** uma operacao e rejeitada por campo obrigatorio vazio
- **THEN** o terminal MUST informar o motivo da rejeicao

### Requirement: Escopo estritamente local de terminal
O sistema MUST ser uma aplicacao local de terminal; MUST NOT exigir interface web, API HTTP propria ou autenticacao de usuario para operar.

#### Scenario: Uso apenas via terminal
- **WHEN** o usuario opera o sistema
- **THEN** todas as interacoes MUST ocorrer pelo menu de terminal local
