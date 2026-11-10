# Proposal

## Why

O FilaFácil cobre coletas em arquivo JSON, inference/manifesto e API de agendamentos, mas nao tem um **painel terminal de auditoria de entregas e reembolsos** com modelo relacional normalizado, joins/paginacao, transacoes, indices, cache Redis e auditoria NoSQL. O case Mundo Dev **Banco de Dados Intermediario** pede exatamente essa lacuna (~6h), sem web/API propria/auth.

## What Changes

- Modelar e seedar tabelas normalizadas: clientes, motoristas, entregas, ocorrencias, reembolsos (PKs/FKs)
- Listagem paginada de entregas (pagina, tamanho, total) com filtros (status, periodo, cliente, motorista, reembolso)
- Queries com INNER JOIN, LEFT JOIN e RIGHT JOIN (ou alternativa documentada se o dialeto limitar)
- Aprovacao de reembolso em **transacao** com rollback em falha; indices em status/data/cliente/motorista documentados
- Cache Redis de consulta frequente (chave, TTL, invalidacao ao atualizar)
- Eventos de auditoria em NoSQL (tipo, usuario, entidade, dados antigos/novos)
- Menus no terminal para listar/filtrar/aprovar/resumo cacheado/consultar auditoria
- Manter coletas JSON, inference, manifesto e API cloud; este modulo e dominio separado (auditoria)
- Premissas: Postgres + Redis + MongoDB (ou equivalente) via Docker Compose; sem web, sem API propria, sem auth

## Capabilities

### New Capabilities

- `audit-relational-model`: schema normalizado clientes/motoristas/entregas/ocorrencias/reembolsos + seeds
- `delivery-query-pagination`: listagem paginada com filtros e joins (INNER/LEFT/RIGHT)
- `refund-transaction`: aprovacao de reembolso transacional com rollback
- `query-cache-redis`: cache Redis com chave, expiracao e invalidacao
- `audit-event-nosql`: eventos de auditoria em armazenamento NoSQL flexivel
- `audit-terminal-menu`: menus terminal para operar consultas, aprovacao e auditoria

### Modified Capabilities

- (nenhuma main spec arquivada; coletas/API existentes nao mudam de requisitos)

## Impact

- Novo pacote (ex.: `com.mundodev.filafacil.auditoria`) + schemas SQL/seed
- Extensao do `docker-compose.yml` (Postgres schema adicional, Redis, MongoDB)
- Opcoes de menu no terminal (sem quebrar 1–9 existentes)
- Dependencias JDBC/Redis/Mongo clients; testes com fakes ou Testcontainers quando viavel
- README: modelo ER, indices, cache, transacao, NoSQL
- Referencia: PDF *Banco de Dados Inter – FilaFácil Logística* (Escola Mundo Dev)
