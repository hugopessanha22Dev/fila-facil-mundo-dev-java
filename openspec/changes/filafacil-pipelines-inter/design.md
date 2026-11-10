# Design

## Context

See `proposal.md` - Why. Ja existe `com.mundodev.filafacil.pipeline` (basico): stages ingest/transform/load, camadas bruta/processada/consolidada em JSON, DuckDB Parquet so na fonte de entregas, cadastro JDBC/CSV, orquestrador sequencial com retries e menu 11. **Nao** ha modo incremental, CDC, checkpoints, upsert/dedup, lineage, late arriving, Parquet na processada nem paralelismo entre fontes.

Constraints:
- Local, sem cluster cloud; volume de fixtures
- Reutilizar DuckDB/Postgres/Jackson; nao quebrar pipeline basico (menu 11)
- `data/` gitignored → fixtures canonicos em `src/main/resources/pipeline/`
- Terminal-first; nova opcao `12` ou flags CLI (`--mode batch|incremental`)

## Goals / Non-Goals

**Goals:**
- Modos batch e incremental sobre o pacote atual
- Quatro fontes com campo de controle CDC e prioridade
- Checkpoint store por fonte; merge/upsert idempotente na consolidada
- Late-arriving policy configuravel; lineage + metricas/alertas
- Parquet particionado na processada; paralelismo por fonte (`ExecutorService`)
- Testes unitarios offline (memory/CSV) cobrindo idempotencia e merge

**Non-Goals:**
- Spark/Flink/Airflow, cluster cloud, Kafka real
- SCD Type 2 completo (historico minimo / auditoria da ultima mudanca basta)
- Dashboard; governanca enterprise
- Alterar semantica de coletas/auditoria/API cloud

## Decisions

### 1. Estender pacote `pipeline` com subpacote `inter`
- **Choice:** `pipeline.inter.*` (CdcClassifier, CheckpointStore, MergeEngine, LineageStore, SourceWorker) chamado por `PipelineInterModule` / flags no `PipelineCli`
- **Why:** Evita fork; basico permanece default no menu 11
- **Alternatives:** Pacote separado `pipelineinter` (duplicacao); substituir basico (quebra case basico)

### 2. Quatro fontes e campos de controle
| Fonte | Adapter | Controle CDC | Prioridade |
|-------|---------|--------------|------------|
| API coletas | HTTP mock local / arquivo JSON gerado | `updated_at` / `version` | 30 |
| Relacional clientes | JDBC `pipe_clientes_unidades` (+ tickets seed) | `updated_at` | 40 |
| Entregas CSV/Parquet | arquivos periodicos | `updated_at` + janela | 20 |
| Motoristas/rotas JSON | SaaS simulado | `updated_at` / `deleted` | 10 |

- **Why:** Espelha o case; prioridade resolve empates de dedup
- **Alternatives:** So arquivos (enfraquece “API” e “relacional”)

### 3. Checkpoint store
- **Choice:** Tabela `pipe_checkpoints(source_id, last_ts, interval_from, interval_to, batch_id, records, status, run_id)` + fallback JSON em `data/pipeline/checkpoints/`
- **Why:** Isolamento por fonte; funciona sem Docker
- **Alternatives:** So memoria (perde reexecucao); um checkpoint global (proibido pelo case)

### 4. Merge/upsert consolidado
- **Choice:** Chave de negocio por entidade (`codigo_coleta`, `id_entrega`, `cliente_codigo`, `motorista_id`); vencedor = maior `updated_at`, depois maior prioridade de fonte; deletes logicos com flag `ativo=false`
- **Why:** Atende CDC + “versao antiga nao sobrescreve”
- **Alternatives:** Last-write-wins sem prioridade (pior em multi-fonte)

### 5. Late arriving
- **Choice:** Env `FILAFACIL_PIPELINE_LATE_POLICY=mark|reject|accept` (default `mark`); atraso = `event_ts` < checkpoint − grace ou < `watermark` da particao
- **Why:** Regra explicita e testavel
- **Alternatives:** Sempre aceitar (silencia problema)

### 6. Processada em Parquet particionado
- **Choice:** DuckDB `COPY ... TO ... (FORMAT PARQUET, PARTITION_BY (dt, regiao))` sob `processada/`
- **Why:** Case exige colunar na processada; DuckDB ja no classpath
- **Alternatives:** So JSON (reprova requisito); Hadoop parquet-writer (footprint)

### 7. Paralelismo
- **Choice:** Pool fixo (default 4) processando adapters de fonte em paralelo; merge serial na carga final
- **Why:** Paralelismo controlado sem race no consolidado
- **Alternatives:** Tudo sequencial (ok funcional, perde peso do case); merge paralelo sem lock (risco)

### 8. Lineage e observabilidade
- **Choice:** `pipe_lineage` / JSONL por run (`source`, `business_key`, `run_id`, `transforms[]`, `dest`); metricas estendidas no `PipelineRunLog`; alertas como linhas `ALERT ...` no log + stderr
- **Why:** Rastreavel e simples

### 9. UX
- **Choice:** Menu `12) Pipeline inter (CDC/incremental)` + CLI `PipelineCli --mode incremental|batch [--source X] [--from --to] [--reprocess]`
- **Why:** Nao altera 0–11

## Risks / Trade-offs

- [Complexidade vs basico] → Mitigacao: subpacote `inter`; basico intacto
- [Postgres offline] → Mitigacao: checkpoints/lineage/merge em arquivos
- [Corrida no merge paralelo] → Mitigacao: merge serial apos futures das fontes
- [Empate de versoes] → Mitigacao: prioridade de fonte documentada
- [Muitos arquivos pequenos no Parquet] → Mitigacao: poucas particoes (dt + regiao) e fixtures pequenos

## Migration Plan

1. Schemas/fixtures inter + CheckpointStore/MergeEngine
2. Adapters das 4 fontes + CDC classifier
3. Stages inter (ingest paralelo, transform/dedup, load upsert, lineage)
4. Menu 12 + CLI flags + testes + README
5. Rollback: remover `pipeline.inter` e opcao 12; menu 11 segue

## Open Questions

- Nenhuma que altere specs: limiar exato de alerta de volume (`FILAFACIL_PIPELINE_ALERT_DISCARD_PCT`) pode ser default 50% na implementacao.
