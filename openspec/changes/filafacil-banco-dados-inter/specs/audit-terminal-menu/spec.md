# Spec Delta

## Purpose

Expor no terminal as operacoes de auditoria (listar/filtrar/aprovar/resumo cacheado/eventos) sem web, API propria ou autenticacao.

## ADDED Requirements

### Requirement: Menus de auditoria no terminal
O aplicativo MUST oferecer opcoes de menu (ou submenu) para listar entregas paginadas, aplicar filtros, aprovar reembolso, visualizar resumo cacheado e consultar eventos NoSQL.

#### Scenario: Navegacao basica
- **WHEN** o usuario abre o menu de auditoria
- **THEN** as operacoes de listagem, aprovacao, resumo e auditoria estao disponiveis sem interface web

### Requirement: Sem API/web/auth neste modulo
O modulo de auditoria MUST operar estritamente via terminal local, sem criar API propria, frontend ou autenticacao de usuarios.

#### Scenario: Restricao de superficie
- **WHEN** o modulo e entregue
- **THEN** nao ha endpoints HTTP novos exclusivos deste case nem tela web de auditoria

### Requirement: Convivencia com o restante do app
As opcoes de auditoria MUST coexistir com o menu existente de coletas/inference/manifesto sem remover funcionalidades anteriores.

#### Scenario: Menu legado intacto
- **WHEN** o usuario usa as opcoes ja existentes de coletas
- **THEN** elas continuam disponiveis apos a inclusao do painel de auditoria
