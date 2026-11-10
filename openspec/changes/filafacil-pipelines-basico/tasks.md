# Tasks

## 1. Estrutura, fixtures e ingestao

- [x] 1.1 Criar pacote `com.mundodev.filafacil.pipeline`, layout `data/pipeline/{input,bruta,processada,consolidada,logs}` e fixtures CSV/JSON/Parquet + schema/seed `pipe_clientes_unidades`; verificar paths e seeds existem e compilam
- [x] 1.2 Adicionar dependencia Parquet (Maven) e implementar leitores/validacao das quatro fontes (CSV, JSON, Parquet, JDBC/fallback); verificar teste de ingestao falha com fonte ausente e sucede com fixtures
- [x] 1.3 Persistir camada bruta particionada por `dt` e `runId`; verificar artefatos sob `bruta/dt=.../run=...` apos ingestao

## 2. Transformacao, metricas e carga

- [x] 2.1 Implementar normalizacao, filtros com contagem de descartados e joins com cadastro; verificar teste cobre descarte contado e enriquecimento
- [x] 2.2 Gerar agregacoes/metricas (coletas, entregas, atrasos, ocorrencias por data/regiao/status/cliente) na camada processada/consolidada; verificar resumo diario presente nos artefatos
- [x] 2.3 Carregar consolidados em arquivos particionados e, se Postgres disponivel, em `pipe_metricas_diarias`; verificar arquivos `consolidada/...` e documentar fallback sem DB

## 3. Orquestracao, terminal e documentacao

- [x] 3.1 Implementar orquestrador sequencial com retries e `PipelineRunLog` (inicio/fim, ms/etapa, lidos/descartados/gravados, status); verificar teste de falha bloqueia etapas seguintes e log registra falha
- [x] 3.2 Expor menu opcao `11` e/ou `PipelineCli` sem alterar menus 0–10; verificar disparo imprime status e resumo no terminal
- [x] 3.3 Atualizar README (camadas, fontes, como rodar, metricas, compose/fallback) e garantir `mvn test` verde com testes do pipeline; verificar comandos documentados

## Workflow follow-up

- Smoke opcional com `docker compose up -d` e carga JDBC real.
- Archive a change (`/opsx-archive`) apos review.
