# Design

## Context

See `proposal.md` - Why. O projeto Java atual tem coletas JSON, manifesto CSV/JSON, auditoria relacional (`aud_*` + Redis/Mongo) e API cloud, mas **nao** tem pipeline batch com camadas, Parquet, orquestracao de ETL nem metricas consolidadas. O case exige batch local (~6h), sem cloud/streaming/dashboard.

Constraints observadas:
- Terminal-first; menus 0–10 ja ocupados (auditoria = 10) → nova opcao `11` ou CLI dedicada `PipelineCli`
- Postgres/HikariCP ja existem; reutilizar para tabela de clientes/unidades do pipeline com schema proprio (`pipe_*`), sem acoplar a `aud_*`
- Jackson cobre CSV/JSON; falta dependencia Parquet
- Testes devem rodar sem Docker (fixtures locais + fallback JDBC/arquivo se DB indisponivel)

## Goals / Non-Goals

**Goals:**
- Pipeline Java em etapas isoladas com camadas bruta/processada/consolidada sob `data/pipeline/`
- Fontes simuladas realistas: CSV coletas, JSON ocorrencias, Parquet entregas, SQL clientes/unidades
- Transformacao com normalizacao, filtros contados, joins e metricas
- Orquestrador simples com retries e log estruturado por execucao
- Menu/CLI para disparar; README com como rodar e interpretar saidas

**Non-Goals:**
- Cloud, streaming, Airflow/Spark, dashboard, governanca avancada
- Reprocessamento incremental complexo / CDC
- Alterar semantica de coletas, auditoria ou API existentes
- Volume de producao; fixtures pequenos bastam

## Decisions

### 1. Pacote `com.mundodev.filafacil.pipeline` com etapas tipadas
- **Choice:** Interfaces `PipelineStage` (`ingest`, `transform`, `load`) + `PipelineOrchestrator` sequencial
- **Why:** Espelha as fases do case e facilita testes por etapa
- **Alternatives:** Script unico monolito (pior testabilidade); Python sidecar (quebra stack Maven unica)

### 2. Layout de dados em disco
```
data/pipeline/
  input/          # fontes simuladas (csv, json, parquet)
  bruta/dt=YYYY-MM-DD/run=<runId>/
  processada/dt=.../run=.../
  consolidada/dt=.../run=.../
  logs/run-<runId>.log
```
- **Why:** Particao por data + versao de execucao atendem o case sem object store
- **Alternatives:** So banco (menos didatico para camadas); so memoria (perde artefatos inspecionaveis)

### 3. Parquet via Apache Parquet + Hadoop shaded minimo ou biblioteca leve
- **Choice:** Dependencia Maven `org.apache.parquet:parquet-hadoop` (+ `hadoop-common` com exclusoes) **ou**, se o footprint for excessivo, gerar/ler Parquet com `parquet-avro` e documentar; fixtures versionados no repo
- **Why:** Case exige Parquet explicitamente; simular so JSON falharia o criterio
- **Alternatives:** Converter Parquet em JSON no fixture (reprova o requisito de formato); DuckDB JNI (mais pesado)

### 4. Tabela relacional `pipe_clientes_unidades` (schema SQL + seed)
- **Choice:** Scripts em `src/main/resources/pipeline/` montados no Postgres do compose; fallback CSV espelho se JDBC falhar (marcado no log)
- **Why:** Case pede tabela relacional para joins; nao misturar com `aud_clientes` (dominios diferentes)
- **Alternatives:** Reusar `aud_clientes` (acoplamento indevido); so arquivo (enfraquece o requisito relacional)

### 5. Carga consolidada: arquivos + upsert opcional em tabela `pipe_metricas_diarias`
- **Choice:** Sempre escrever JSON/CSV consolidado; se Postgres ok, tambem carregar metricas
- **Why:** Arquivos garantem demos offline; tabela cobre “carga em tabela relacional”
- **Alternatives:** So arquivo; so banco

### 6. Orquestracao e retries
- **Choice:** Loop simples com `maxRetries` por etapa (default 1–2), backoff curto; erro permanente propaga
- **Why:** Case pede retries sem orquestrador corporativo
- **Alternatives:** Spring Batch / Airflow (fora do escopo e peso)

### 7. Observabilidade
- **Choice:** `PipelineRunLog` (objeto + arquivo texto) com inicio/fim, ms por etapa, lidos/descartados/gravados, status
- **Why:** Criterio de sucesso do PDF; legivel no terminal

### 8. Integracao UX
- **Choice:** Opcao de menu `11) Pipeline batch` + `PipelineCli` main class Maven
- **Why:** Alinha aos outros modulos; CLI ajuda CI/smoke sem Scanner interativo

## Risks / Trade-offs

- [Parquet/Hadoop pesado no classpath] → Mitigacao: exclusoes Maven agressivas; fixture pequeno; documentar versoes
- [Docker Postgres offline] → Mitigacao: fallback de cadastro via CSV seed; testes unitarios sem DB
- [Perda silenciosa de registros] → Mitigacao: contadores obrigatorios + assert em testes de limpeza
- [Colisao com menus/opcoes] → Mitigacao: nova opcao 11; nao alterar 0–10
- [Acoplamento acidental a auditoria] → Mitigacao: prefixo `pipe_` e pacote isolado

## Migration Plan

1. Adicionar deps + fixtures + schema seed
2. Implementar stages e orquestrador
3. Wire menu/CLI + README
4. `mvn test` deve passar sem Docker; smoke opcional com compose
5. Rollback: remover pacote/opcao 11/deps; dados em `data/pipeline/` sao geraveis

## Open Questions

- Nenhuma que altere specs: footprint exato da lib Parquet sera validado na implementacao; se Maven ficar inviavel, documentar wrapper minimo mantendo formato `.parquet` real.
