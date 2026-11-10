# Tasks

## 1. Modelo relacional e infraestrutura

- [x] 1.1 Criar schema SQL normalizado (clientes, motoristas, entregas, ocorrencias, reembolsos) com PKs/FKs e indices; verificar scripts aplicaveis no Postgres do compose
- [x] 1.2 Adicionar seeds e estender `docker-compose.yml` com Redis + MongoDB (mantendo Postgres); verificar servicos sobem e seeds carregam quando o Docker estiver disponivel
- [x] 1.3 Criar pacote `auditoria` (models + JDBC repos basicos) e wiring por env; verificar compilacao e leitura simples de contagens das tabelas

## 2. Consultas, joins e paginacao

- [x] 2.1 Implementar listagem paginada de entregas com filtros (status, periodo, cliente, motorista, reembolso); verificar pagina/tamanho/total em teste ou smoke SQL
- [x] 2.2 Implementar queries com INNER JOIN, LEFT JOIN e RIGHT JOIN (ou alternativa documentada); verificar cenarios com e sem ocorrencia
- [x] 2.3 Documentar indices e justificativas no README; verificar nomes dos indices batem com o schema

## 3. Transacao, Redis, NoSQL e menu

- [x] 3.1 Implementar aprovacao de reembolso em transacao com rollback; verificar teste/simulacao de falha nao deixa estado parcial
- [x] 3.2 Integrar cache Redis (chave, TTL, invalidacao na aprovacao) e store NoSQL de eventos de auditoria; verificar hit/invalidacao e gravacao/listagem de eventos
- [x] 3.3 Adicionar submenu/opcao de auditoria no terminal (listar, aprovar, resumo cacheado, eventos) sem remover menus existentes; verificar fluxo manual documentado e `mvn test` da suite atual permanece verde
- [x] 3.4 Atualizar README com modelo relacional, joins, paginacao, transacao, Redis e NoSQL; verificar instrucoes de compose/comandos reais

## Workflow follow-up

- Validar compose completo com Docker Desktop ativo (`docker compose up -d`).
- Archive a change (`/opsx-archive`) apos review.
