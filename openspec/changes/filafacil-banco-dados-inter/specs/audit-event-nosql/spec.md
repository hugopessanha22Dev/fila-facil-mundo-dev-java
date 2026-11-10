# Spec Delta

## Purpose

Registrar eventos de auditoria em armazenamento NoSQL com estrutura flexivel, separado do modelo relacional principal.

## ADDED Requirements

### Requirement: Persistencia NoSQL de eventos
O sistema MUST gravar eventos de auditoria em NoSQL contendo tipo, usuario responsavel, entidade afetada e dados anteriores/novos (estrutura flexivel, sem schema relacional fixo).

#### Scenario: Evento apos aprovacao
- **WHEN** um reembolso e aprovado com sucesso
- **THEN** um evento de auditoria e persistido no NoSQL com tipo e entidade do reembolso

### Requirement: Consulta de eventos no terminal
O usuario MUST poder listar/consultar eventos de auditoria NoSQL pelo menu terminal.

#### Scenario: Listagem de eventos
- **WHEN** o usuario escolhe a opcao de consultar auditoria
- **THEN** o terminal exibe eventos recentes com tipo, usuario e entidade afetada

### Requirement: Separacao de responsabilidades
O NoSQL MUST NOT substituir o banco relacional como fonte principal de entregas/reembolsos.

#### Scenario: Fonte da verdade relacional
- **WHEN** o usuario lista entregas paginadas
- **THEN** os dados estruturados vem do banco relacional, nao do store NoSQL
