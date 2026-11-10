# Proposal

## Why

A FilaFácil recebe diariamente coletas, entregas, ocorrencias e cadastros em formatos diferentes, mas o repositorio ainda nao tem um **pipeline batch local** com camadas (bruta/processada/consolidada), ingestao multi-fonte (CSV/JSON/Parquet + tabela relacional), transformacao/normalizacao, carga particionada, orquestracao com retries e logs de execucao. O case Mundo Dev **Data Engineering & Pipelines Basico** cobre exatamente essa lacuna (~6h), sem cloud, streaming ou dashboard.

## What Changes

- Criar pipeline batch local em Java com etapas isoladas: ingestao/validacao, transformacao/normalizacao, carga/organizacao
- Consumir fontes simuladas: CSV (coletas), JSON (ocorrencias), Parquet (entregas) e tabela relacional (clientes/unidades) para enriquecimento
- Organizar saidas em camadas `bruta`, `processada` e `consolidada`, com particionamento por data e versao de execucao
- Aplicar filtros/limpeza, joins com cadastro, agregacoes e metricas operacionais (totais por data/regiao/status/cliente; atrasos; ocorrencias)
- Orquestrar etapas com dependencias, retries em falhas transitivas e logs (inicio/fim, tempo por etapa, lidos/descartados/gravados) sem perda silenciosa de registros
- Expor execucao via CLI/menu terminal (sem web/API propria); manter coletas JSON, inference, manifesto, auditoria e API cloud intactos
- Premissas: stack minima Java + arquivos locais + Postgres existente (ou fallback em arquivo); sem cloud, streaming, dashboard, orquestrador corporativo ou banco distribuido

## Capabilities

### New Capabilities

- `pipeline-batch-layers`: camadas bruta/processada/consolidada com particionamento por data e versionamento de execucao
- `pipeline-multi-source-ingest`: ingestao e validacao de CSV, JSON, Parquet e tabela relacional
- `pipeline-transform-metrics`: normalizacao, filtros, joins, agregacoes e metricas operacionais
- `pipeline-orchestration-observability`: orquestracao sequencial com retries e logs de execucao rastreaveis
- `pipeline-terminal-run`: disparo e acompanhamento do pipeline pelo terminal/CLI

### Modified Capabilities

- (nenhuma main spec arquivada; coletas/auditoria/API existentes nao mudam de requisitos)

## Impact

- Novo pacote (ex.: `com.mundodev.filafacil.pipeline`) + fixtures sob `data/pipeline/`
- Dependencia de leitura Parquet (ex.: Apache Parquet/Avro ou equivalente leve) + reuso de Jackson/JDBC
- Tabela relacional de clientes/unidades dedicada ao pipeline (ou seed proprio; nao acoplar ao schema `aud_*` de auditoria)
- Opcao de menu/CLI para executar o pipeline sem remover menus 0–10 existentes
- Testes unitarios das etapas com fixtures locais; README com camadas, fontes, metricas e como rodar
- Referencia: PDF *Data Engineering & Pipelines Basico – FilaFácil Logística* (Escola Mundo Dev)
