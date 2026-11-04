# Spec Delta

## Purpose

Define o menu interativo de terminal do FilaFacil no case de software basico, com loop continuo e opcoes de cadastro, listagem, filtros, atualizacao, resumo e saida.

## ADDED Requirements

### Requirement: Menu interativo em loop
A aplicacao MUST apresentar um menu no terminal em loop ate o usuario sair, com opcoes para cadastrar, listar, filtrar, atualizar status, gerar resumo e sair.

#### Scenario: Exibir opcoes
- **WHEN** a aplicacao inicia com sucesso
- **THEN** o usuario MUST ver o menu com as operacoes do case, incluindo resumo e sair

#### Scenario: Continuar apos operacao
- **WHEN** o usuario conclui uma operacao diferente de sair
- **THEN** o sistema MUST voltar ao menu sem encerrar

#### Scenario: Sair
- **WHEN** o usuario escolhe sair
- **THEN** o loop MUST encerrar de forma controlada

### Requirement: Opcao invalida nao encerra
O sistema MUST validar a escolha do menu e, em opcao invalida, informar o erro e continuar o loop sem encerrar o programa.

#### Scenario: Opcao desconhecida
- **WHEN** o usuario informa uma opcao inexistente
- **THEN** o sistema MUST avisar que a opcao e invalida e MUST reexibir o menu

### Requirement: Escopo estritamente terminal local
O sistema MUST ser estritamente uma aplicacao local de terminal e MUST NOT exigir interface web, banco de dados, frameworks de aplicacao, API propria ou autenticacao.

#### Scenario: Uso apenas no terminal
- **WHEN** o usuario opera o FilaFacil
- **THEN** todas as interacoes MUST ocorrer pelo menu de terminal local
