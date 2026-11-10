# Design

## Context

Codigo atual: coletas em JSON (`coletas` feature-based), inference/manifesto, API Javalin de agendamentos com Postgres em `docker-compose`. Observado: sem dominio entregas/reembolsos, sem Redis, sem NoSQL de auditoria, sem paginacao/joins no terminal. Ver `proposal.md` — Why.

Restricoes do case: ~6h, terminal only, sem web/API propria/auth/microsservicos/filas; Redis e bancos podem ser containers locais.

## Goals / Non-Goals

**Goals:**

- Dominio `auditoria` no terminal com Postgres normalizado + seeds
- Paginacao + filtros + INNER/LEFT/RIGHT JOIN
- Transacao de aprovacao + indices documentados
- Redis cache (chave/TTL/invalidacao) + MongoDB (NoSQL) para eventos
- README completo; nao quebrar coletas/inference/manifest/API

**Non-Goals:**

- Substituir coletas JSON por este modelo
- Expor auditoria via Javalin/API cloud
- Dashboards graficos, auth, dados reais de clientes

## Decisions

### 1. Dominio separado `com.mundodev.filafacil.auditoria`

```
auditoria/
  model/ (Cliente, Motorista, Entrega, Ocorrencia, Reembolso, AuditEvent)
  repo/ (JDBC + RedisCache + MongoAuditStore)
  service/ (EntregaQueryService, ReembolsoService, ResumoCacheService)
  menu/ (AuditoriaMenu ou integracao MenuController)
```

- Composition root: `Main` wiring opcional se env `FILAFACIL_AUDITORIA=on` ou sempre disponivel quando Postgres/Redis/Mongo sobem
- **Por que:** case e dominio proprio; evita misturar com coletas/API
- **Alternativa:** migrar coletas para o mesmo ER — rejeitada (escopo/case diferentes)

### 2. Stack de persistencia (Compose)

Estender `docker-compose.yml`:

| Servico | Uso |
|---------|-----|
| Postgres (ja existe) | Schema `auditoria` ou prefixo tabelas `aud_*` + seed SQL |
| Redis | Cache do resumo/consulta frequente |
| MongoDB | Collection `audit_events` |

Schema separado do `agendamentos` da API (`02-auditoria-schema.sql`, `03-auditoria-seed.sql` em initdb).

### 3. Joins e paginacao

- `EntregaQueryService.listar(filtro, page, size)` → SQL com `COUNT(*) OVER()` ou count separado + `LIMIT/OFFSET`
- INNER JOIN cliente/motorista; LEFT JOIN ocorrencia; RIGHT JOIN documentado (ex.: motoristas sem entrega no periodo — ou equivalente Postgres documentado no README se RIGHT for raro)
- Filtros: status, data_ini/data_fim, cliente_id, motorista_id, tem_reembolso

### 4. Transacao de aprovacao

```
BEGIN
  UPDATE reembolsos SET status='aprovado', ... WHERE id=? AND status='pendente'
  (opcional) UPDATE entregas / registrar ocorrencia
  COMMIT
ON ERROR ROLLBACK
```

Apos sucesso: invalidar cache Redis + gravar evento Mongo.

### 5. Redis

- Chave: `filafacil:auditoria:resumo:v1` (ou hash por filtro canonico)
- TTL: 60s (configuravel via env)
- Invalidacao: `DEL` na aprovacao e em seeds/updates relevantes
- Fallback: se Redis indisponivel em local, logar aviso e consultar Postgres (documentar)

### 6. MongoDB NoSQL

Documento flexivel:

```json
{
  "tipo": "REEMBOLSO_APROVADO",
  "usuario": "terminal-local",
  "entidade": "reembolso",
  "entidadeId": "...",
  "dadosAntigos": {...},
  "dadosNovos": {...},
  "em": "ISO-8601"
}
```

Driver oficial Mongo; sem usar Mongo como fonte de entregas.

### 7. Menu terminal

Submenu (ex. opcao `A` ou `10` — Auditoria):

1. Listar entregas (pagina/tamanho + filtros)
2. Aprovar reembolso
3. Resumo cacheado
4. Eventos de auditoria
5. Voltar

Nao criar HTTP endpoints novos para este case.

### 8. Testes

- Unitarios de paginacao/filtros com repository fake quando possivel
- Integracao opcional se Docker disponivel; nao falhar CI manifesto se containers ausentes (perfil `auditoria-it` ou testes unitarios puros + SQL validado)

## Risks / Trade-offs

- [Docker Desktop offline] → Mitigation: scripts/schema versionados; modo degradado documentado; CI nao depende de Redis/Mongo
- [Confusao com API Postgres de agendamentos] → Mitigation: schemas/scripts separados e README claro
- [Escopo >6h] → Mitigation: seed minimo, um cache key, um fluxo de aprovacao
- [RIGHT JOIN pouco usado em Postgres] → Mitigation: query real + nota no README se alternativa

## Migration Plan

1. SQL schema/seed + Compose Redis/Mongo
2. Repos JDBC + services + menu
3. Transacao + Redis + Mongo
4. README + testes; rollback remove submenu e containers extras

## Open Questions

- Nenhuma bloqueante: Postgres+Redis+Mongo via Compose; menu opcao dedicada; fallback sem Redis documentado.
