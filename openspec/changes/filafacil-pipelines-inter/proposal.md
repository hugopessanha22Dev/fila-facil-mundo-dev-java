# Proposal

## Why

O pipeline batch basico da FilaFácil recarrega o historico inteiro a cada execucao, sem CDC, checkpoints, upsert/deduplicacao, late arriving data, lineage nem idempotencia — gerando duplicidades, lentidao e falta de rastreabilidade. O case Mundo Dev **Data Engineering & Pipelines Intermediario** cobre exatamente essa evolucao (~6h), sem cluster cloud real.

## What Changes

- Estender o pipeline existente para modos **batch completo** e **incremental** (campo de controle / CDC simulado por fonte)
- Integrar ao menos quatro fontes com frequencias distintas: API de coletas (simulada), banco relacional de clientes, CSV/Parquet de entregas, JSON de motoristas/rotas (SaaS simulado)
- Aplicar deduplicacao por chave de negocio + timestamp + prioridade de fonte; upsert/merge na camada final
- Tratar late arriving data com regra explicita (aceitar/rejeitar/marcar) e reprocessamento idempotente por particao/periodo/fonte
- Persistir **checkpoints por fonte** (ultimo ponto, intervalo, status, lote, volume) sem misturar estados
- Registrar **lineage** (origem → transformacoes → destino) e auditoria por execucao (fontes, intervalo, checkpoint, volumes, status)
- Usar Parquet na camada processada com particionamento (data/regiao/status) e paralelismo controlado entre fontes/particoes
- Ampliar logs/metricas (lidos, descartados, inseridos, atualizados, deduplicados, atrasados, tempo/etapa, alertas de falha/volume) e documentacao no README
- Manter menus 0–11 e pipeline basico funcionando; expor modo inter via CLI/menu (ex.: opcao 12 ou flags `--mode incremental|batch`)
- Premissas: local, sem cluster cloud, sem volume massivo real; reutilizar pacote `pipeline` e Postgres/DuckDB ja presentes

## Capabilities

### New Capabilities

- `pipeline-incremental-cdc`: carga batch + incremental com CDC simulado (insert/update/delete logico) por campo de controle
- `pipeline-upsert-dedup`: deduplicacao e upsert/merge na camada final com chave de negocio e versao
- `pipeline-checkpoints-idempotency`: checkpoints por fonte e reexecucao/reprocessamento idempotente
- `pipeline-late-arriving`: regras para dados atrasados e reprocessamento controlado
- `pipeline-lineage-observability`: lineage, metricas/alertas estruturados e auditoria por execucao
- `pipeline-inter-sources`: quatro fontes integradas (API, relacional, CSV/Parquet, JSON) com particionamento colunar e paralelismo controlado

### Modified Capabilities

- (nenhuma main spec arquivada; o change `filafacil-pipelines-basico` ainda nao foi arquivado para `openspec/specs/` — este change adiciona capacidades novas sobre o pacote existente)

## Impact

- Extensao de `com.mundodev.filafacil.pipeline` (novos stages/stores: checkpoint, lineage, merge, CDC)
- Fixtures e schemas adicionais (`pipe_checkpoints`, `pipe_lineage`, entidades motoristas/rotas, API mock de coletas)
- Parquet na camada processada; paralelismo (ex.: `ExecutorService` por fonte)
- Menu/CLI com modo incremental; testes de idempotencia, late arriving e merge
- README tecnico do fluxo inter
- Referencia: PDF *Data Engineering & Pipelines Intermediario – FilaFácil Logística* (Escola Mundo Dev)
