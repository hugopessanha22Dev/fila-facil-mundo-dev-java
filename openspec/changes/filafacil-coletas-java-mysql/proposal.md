# Proposal

## Why

A FilaFácil Logística deixou de caber em persistência JSON local: o volume de coletas exige consultas, atualizações e exclusões com segurança e filtros estruturados. Este change inicia o projeto Java + MySQL (`fila-facil`) como aplicação de terminal com ORM, alinhada ao case Mundo Dev de banco de dados básico.

## What Changes

- Criar aplicação Java local de terminal (sem web, sem API HTTP, sem autenticação)
- Provisionar banco MySQL local chamado `fila-facil` com uma única tabela de coletas
- Mapear a tabela via ORM e concentrar todo acesso a dados na camada repository
- Implementar CRUD completo: cadastrar, listar (ordenado), atualizar status por ID, deletar com verificação de existência
- Implementar filtros equivalentes a WHERE por status, prioridade e nome do cliente
- Separar camadas controller (menu), service (validações/regras) e repository (ORM)
- Incluir README com configuração do banco, execução e operações principais
- Entregar histórico Git com commits incrementais e push remoto quando aplicável

## Capabilities

### New Capabilities

- `coleta-management`: ciclo de vida das coletas no banco relacional — estrutura dos campos, cadastro com validação, listagem ordenada, filtros por status/prioridade/nome, atualização de status e exclusão com checagem de existência
- `terminal-cli`: menu interativo em loop no terminal, com delegação controller → service → repository e sem lógica de banco fora do repository

### Modified Capabilities

- (nenhuma — projeto greenfield sem specs existentes)

## Impact

- Repositório atualmente sem código de aplicação; será bootstrap completo (build Java/Maven ou equivalente, ORM, driver MySQL)
- Dependências novas: JDK, MySQL local, ORM escolhido no design (ex.: Hibernate/JPA)
- Nenhuma API externa, múltiplas tabelas, joins ou autenticação entram no escopo
- Artefato de referência: case PDF *Banco de Dados Básico – FilaFácil Logística* (Escola Mundo Dev)
