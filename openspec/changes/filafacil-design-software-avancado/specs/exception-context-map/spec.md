# Spec Delta

## Purpose

Delimitar gestão de exceções, roteirização, atendimento, financeiro e auditoria, e traduzir a linguagem de cada um antes de entrar no domínio.

## ADDED Requirements

### Requirement: Cinco contextos documentados
O sistema MUST documentar a responsabilidade de gestão de exceções, roteirização, atendimento, financeiro e auditoria operacional, e MUST indicar o sentido upstream ou downstream de cada relação com a gestão de exceções.

#### Scenario: Mapa publicado
- **WHEN** o mapa de contextos é lido
- **THEN** os cinco nomes aparecem, a gestão de exceções é o contexto principal e a auditoria consta como downstream

### Requirement: Linguagem publicada e camada anticorrupcao
O sistema MUST traduzir termos de roteirização, atendimento e financeiro para tipo e origem da exceção, e MUST recusar termo que não tenha tradução.

#### Scenario: Rota bloqueada
- **WHEN** o gateway recebe o termo de rota bloqueada vindo da roteirização
- **THEN** o domínio recebe o tipo rota bloqueada e a origem roteirização

#### Scenario: Termo desconhecido
- **WHEN** o gateway recebe um termo sem tradução
- **THEN** nenhuma exceção é aberta com esse termo cru
