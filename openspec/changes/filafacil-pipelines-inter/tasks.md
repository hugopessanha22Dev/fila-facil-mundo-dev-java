# Tasks

## 1. Fontes, CDC e checkpoints

- [x] 1.1 Criar subpacote `pipeline.inter`, fixtures das 4 fontes (API mock coletas, JDBC clientes, entregas CSV/Parquet, JSON motoristas/rotas) e schema `pipe_checkpoints`/`pipe_lineage`; verificar materializacao e scripts SQL existem
- [x] 1.2 Implementar adapters + CDC classifier (insert/update/delete) e modos batch/incremental por campo de controle; verificar teste de incremental le so o delta e batch le tudo
- [x] 1.3 Implementar CheckpointStore (JDBC + fallback arquivo) isolado por fonte; verificar falha nao avanca checkpoint e sucesso persiste intervalo/volume

## 2. Dedup, late arriving, merge e Parquet

- [x] 2.1 Implementar deduplicacao (chave + timestamp + prioridade) e MergeEngine upsert na consolidada; verificar versao antiga nao sobrescreve e contagens inserido/atualizado/deduplicado
- [x] 2.2 Implementar late-arriving policy (`mark|reject|accept`) e reprocessamento por fonte/periodo idempotente; verificar teste de registro atrasado e segunda execucao sem duplicar
- [x] 2.3 Gravar camada processada em Parquet particionado (dt + regiao) via DuckDB e paralelizar ingestao por fonte; verificar arquivos Parquet e log com volumes por fonte

## 3. Lineage, UX e documentacao

- [x] 3.1 Registrar lineage + auditoria/metricas/alertas por execucao; verificar relatorio inclui fontes, intervalo, checkpoints, volumes e ALERT em falha/volume zero
- [x] 3.2 Expor menu opcao `12` e CLI flags (`--mode`, `--reprocess`) sem alterar menus 0–11; verificar disparo imprime status e resumo
- [x] 3.3 Atualizar README (CDC, checkpoints, merge, late arriving, lineage, Parquet, como rodar) e garantir `mvn test` verde com testes inter; verificar comandos documentados

## Workflow follow-up

- Smoke opcional com Postgres (`docker compose up -d`) para checkpoints/lineage JDBC.
- Archive a change (`/opsx-archive`) apos review.
