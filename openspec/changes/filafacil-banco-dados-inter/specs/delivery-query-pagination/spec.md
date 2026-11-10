# Spec Delta

## Purpose

Permitir consultar entregas com paginacao, filtros combinados e joins (INNER, LEFT, RIGHT) para auditoria operacional no terminal.

## ADDED Requirements

### Requirement: Listagem paginada
A listagem de entregas MUST aceitar pagina e tamanho e retornar o total de registros (ou indicar continuidade de paginas) mantendo filtros aplicados.

#### Scenario: Segunda pagina
- **WHEN** o usuario solicita pagina 2 com tamanho N e ha mais de N entregas filtradas
- **THEN** o sistema retorna o proximo lote e o total (ou sinal de proximas paginas)

### Requirement: Filtros combinados
As consultas MUST permitir filtrar por status, periodo, cliente, motorista e existencia/status de reembolso.

#### Scenario: Filtro por status e periodo
- **WHEN** o usuario informa status e intervalo de datas validos
- **THEN** apenas entregas que satisfazem ambos os criterios entram no resultado paginado

### Requirement: Joins obrigatorios e opcionais
As queries MUST usar INNER JOIN quando cliente e motorista forem obrigatorios, LEFT JOIN para incluir entregas sem ocorrencia, e RIGHT JOIN (ou alternativa documentada) quando o dialeto/caso exigir.

#### Scenario: Entrega sem ocorrencia
- **WHEN** uma entrega nao possui ocorrencia e a consulta usa LEFT JOIN
- **THEN** a entrega ainda aparece no resultado com campos de ocorrencia nulos/ausentes
