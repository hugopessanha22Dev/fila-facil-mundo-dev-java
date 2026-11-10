# FilaFacil Logistica

Aplicação Java de **terminal** para gerenciar coletas com:

- Persistência local em JSON (`data/coletas.json`)
- Enriquecimento assíncrono via **ViaCEP** no cadastro
- Estado centralizado (`AppState`)
- Erros tipados (entrada / arquivo / API / inference)
- **Triagem operacional com IA** (gateway, roteamento, JSON schema, tools mock, eval offline)
- **Prompt engineering**: biblioteca de prompts, few-shot, atendimento conversacional com memoria e grounding
- **Validacao de manifesto** (JSON/CSV) com relatorio aprovado/reprovado e CLI para CI
- **API HTTP de agendamento de docas** (Javalin + Postgres + storage) pronta para cloud AWS
- Testes unitários (JUnit 5)
- Pipeline **GitHub Actions** (test + smoke + artefato)

Cases: Escola Mundo Dev — Módulo 2 + Modelos & Inference + Prompt Engineering + DevOps Fundamentals + Cloud para Backend + Design de Software Inter + Banco de Dados Inter + Data Engineering & Pipelines Basico + Pipelines Intermediario.

## Pré-requisitos

- JDK 17+ (o Maven do ambiente tipicamente já usa 17)
- Apache Maven 3.9+
- Acesso à internet para a ViaCEP (cadastro real)
- (Opcional) chave de API OpenAI-compatible para modo HTTP
- (API cloud) Docker para Postgres local; conta AWS para deploy

## Executar

```powershell
# Padrao: modo mock (sem chave / sem rede de LLM)
mvn -q compile exec:java
```

### API de agendamento de docas

```powershell
# 1) Banco local (requer Docker Desktop)
docker compose up -d

# 2) Env (copie .env.example -> .env; a API tambem le variaveis do shell)
# FILAFACIL_API_MODE=local FILAFACIL_API_PORT=8080 ...

# 3) Subir API
mvn -q compile exec:java@api

# Smoke sem Docker (repositorio em memoria):
$env:FILAFACIL_DB_URL = "memory"
mvn -q compile exec:java@api
```

Health: `GET http://localhost:8080/health`

## Testes

```powershell
mvn clean test
```

Os testes usam fake de `CepApiClient` e gateway LLM em mock (não chamam a rede). Na primeira vez apos mudar de JDK, use `clean` para recompilar.

### Eval offline (golden dataset)

```powershell
mvn -q exec:java@offline-eval
```

Ou via teste: `OfflineEvalRunnerTest` (incluido em `mvn test`).

## Variaveis de ambiente LLM (`FILAFACIL_LLM_*`)

Nunca coloque chaves reais no codigo ou no README.

| Variavel | Default | Descricao |
|----------|---------|-----------|
| `FILAFACIL_LLM_MODE` | `mock` | `mock` (sem rede) ou `http` |
| `FILAFACIL_LLM_API_KEY` | (vazio) | Bearer token do provedor (obrigatoria em `http`) |
| `FILAFACIL_LLM_BASE_URL` | `https://api.openai.com/v1` | Base OpenAI-compatible (OpenAI, Groq, etc.) |
| `FILAFACIL_LLM_MODEL_LIGHT` | `gpt-4o-mini` | Perfil leve (triagem barata/rapida) |
| `FILAFACIL_LLM_MODEL_DETAILED` | `gpt-4o` | Perfil detalhado (casos criticos) |
| `FILAFACIL_LLM_TIMEOUT_SECONDS` | `20` | Timeout HTTP |
| `FILAFACIL_LLM_MAX_RETRIES` | `2` | Retries em falhas transitorias |
| `FILAFACIL_LLM_PRICE_PER_1K` | `0.00015` | Estimativa de custo USD / 1k tokens |

Exemplo PowerShell (HTTP):

```powershell
$env:FILAFACIL_LLM_MODE = "http"
$env:FILAFACIL_LLM_API_KEY = "sua-chave"
$env:FILAFACIL_LLM_BASE_URL = "https://api.openai.com/v1"
mvn -q compile exec:java
```

## Comparacao de perfis de modelo

| Dimensao | LIGHT (`MODEL_LIGHT`) | DETAILED (`MODEL_DETAILED`) |
|----------|------------------------|------------------------------|
| Uso | Consultas simples / baixa criticidade | Reclamacoes, urgencia, texto longo |
| Latencia | Menor (prioridade) | Maior |
| Custo | Menor | Maior |
| Qualidade | Suficiente para classificacao | Melhor em ambiguidades |
| Temperature | ~0.4 | ~0.1 (previsibilidade) |

Roteamento automatico: heuristica local (palavras de urgencia/reclamacao e tamanho do texto). Sem escolha manual no fluxo padrao. Fallback: se o perfil leve falhar, tenta o detalhado.

## API REST utilizada

- **ViaCEP:** `https://viacep.com.br/ws/{cep}/json/`
- No cadastro, o sistema pede o CEP, consulta a API com `CompletableFuture` e só persiste após sucesso.
- Em falha (CEP inválido/rede), o cadastro **não** é gravado e o menu continua.

## Persistência

Arquivo: `data/coletas.json`

Campos: `id`, `nome`, `endereco`, `pacotes`, `prioridade`, `status`, `cep`, `bairro`, `cidade`, `uf`

## Menu

| Opção | Ação |
|-------|------|
| 1 | Cadastrar (com CEP / ViaCEP) |
| 2 | Listar |
| 3 | Filtrar (status ou prioridade) |
| 4 | Atualizar status |
| 5 | Resumo operacional |
| 6 | Triagem IA (mensagem → JSON validado) |
| 7 | Atendimento conversacional (multi-turno + clarificacao) |
| 8 | Validar manifesto (JSON/CSV + relatorio) |
| 9 | Deletar coleta |
| 10 | Auditoria entregas/reembolsos (submenu) |
| 11 | Pipeline batch (ETL multi-fonte) |
| 12 | Pipeline inter (CDC/incremental) |
| 0 | Sair |

No atendimento conversacional: `/reset` limpa a sessao; `/fim` encerra sem concluir.

### Valores

- **Prioridade:** `baixa`, `media`, `alta`
- **Status:** `pendente`, `em_rota`, `concluida`

## Prompt library e few-shot

Resources versionados:

- `src/main/resources/inference/prompts/` — `classify`, `extract`, `summarize`, `internal-reply`, `transform`
- `src/main/resources/inference/fewshot/catalog.json` — exemplos positivo / negativo / contraexemplo / escalacao (dados ficticios)

Cada prompt declara objetivo, contexto, restricoes, criterios e formato de saida. O `PromptComposer` monta system prompt = template + few-shot + memoria da sessao.

## Fluxo conversacional e grounding

1. Recepcao: identifica intencao e campos ja informados
2. Clarificacao: pergunta so o que falta (sem repetir)
3. Conclusao: JSON estruturado so com campos preenchidos ou marcados desconhecido
4. Grounding: nao inventar status/prazo/codigo; origem `fato|inferencia|ausente`; retry orientado (max 2) ou revisao humana

## Validacao de manifesto (DevOps)

Fixtures locais:

- `data/manifestos/valido.json` / `valido.csv`
- `data/manifestos/invalido.json` (duplicata + campos vazios)

Regras: campos obrigatorios, pacote unico, rota/veiculo/responsavel presentes. Relatorio com status `aprovado` ou `reprovado`.

### CLI (CI / headless)

```powershell
# Exit 0 = aprovado
mvn -q compile exec:java@manifest-cli "-Dexec.args=data/manifestos/valido.json"

# Exit 1 = reprovado
mvn -q exec:java@manifest-cli "-Dexec.args=data/manifestos/invalido.json"

# Salvar relatorio
mvn -q exec:java@manifest-cli "-Dexec.args=data/manifestos/valido.json --out data/manifestos/ultimo-relatorio.txt"
```

No menu interativo: opcao **8**.

## Cloud Backend (API de docas)

API HTTP separada do terminal (`com.mundodev.filafacil.api.ApiMain`). Sem frontend e sem auth completa.

### Endpoints

| Metodo | Rota | Acao |
|--------|------|------|
| GET | `/health` | disponibilidade |
| POST | `/agendamentos` | criar (transportadora, placa, doca, dataHora) |
| GET | `/agendamentos` | listar |
| GET | `/agendamentos?data=YYYY-MM-DD` | filtrar por data |
| PATCH | `/agendamentos/{id}/status` | atualizar status |
| POST | `/agendamentos/{id}/cancelar` | cancelar |
| POST | `/agendamentos/{id}/anexo` | multipart `file` (limite ~2 MB) |

Status: `agendado`, `em_atendimento`, `concluido`, `cancelado`.

### Variaveis de ambiente (`FILAFACIL_API_*`)

Nunca commite `.env` com secrets. Use `.env.example` como modelo.

| Variavel | Default local | Descricao |
|----------|---------------|-----------|
| `FILAFACIL_API_MODE` | `local` | `local` (filesystem anexos) ou `prod` (S3) |
| `FILAFACIL_API_PORT` | `8080` | Porta HTTP |
| `FILAFACIL_DB_URL` | `jdbc:postgresql://localhost:5432/filafacil` | JDBC Postgres (RDS em prod); use `memory` para smoke sem Docker |
| `FILAFACIL_DB_USER` | `filafacil` | Usuario DB |
| `FILAFACIL_DB_PASSWORD` | `filafacil` | Senha DB (so via env/secret) |
| `FILAFACIL_S3_BUCKET` | (vazio) | Bucket S3 obrigatorio em `prod` |
| `FILAFACIL_AWS_REGION` | `us-east-1` | Regiao AWS |
| `FILAFACIL_ANEXOS_DIR` | `data/anexos` | Dir local de anexos em `local` |
| `FILAFACIL_MAX_ANEXO_BYTES` | `2097152` | Limite de upload |

Storage: em `local` grava em disco e salva `anexo_ref` tipo `file://...`; em `prod` envia ao S3 e salva `s3://bucket/key`. O banco guarda **somente a referencia**.

### Deploy AWS (checklist dos 8 requisitos)

1. **Env**: configure as vars acima no servico (App Runner / ECS). Secrets de DB via Secrets Manager.
2. **Banco**: RDS Postgres; security group permitindo a origem da API; schema em `src/main/resources/api/schema.sql`.
3. **Storage**: bucket S3; IAM so no prefixo `agendamentos/*` (ver `infra/iam-api-role-policy.json`).
4. **Deploy**: empacote o jar (`mvn -B package`) e publique no **AWS App Runner** (padrao) apontando para `ApiMain`, ou **ECS Fargate**.
5. **Load balancer**: App Runner expoe URL HTTPS com LB gerenciado; alternativa ECS + **ALB** com target no container e health `GET /health`.
6. **Auto scaling**: min=1 max=3, metrica CPU ~70% — ver `infra/auto-scaling.json`.
7. **IAM**: role da tarefa/servico com policy minima (DB secret, S3 anexos, CloudWatch Logs) — sem admin amplo.
8. **Logs**: stdout da JVM (slf4j-simple) coletado no CloudWatch; registrar startup, method/path, erros DB/S3 e health **sem** senhas/tokens/PII.

Checklist pos-deploy: `curl https://<url-publica>/health` deve retornar `{"status":"ok"}`.

Detalhes extras: `infra/README.md`.

## CI vs deploy

| Fluxo | O que faz | Onde |
|-------|-----------|------|
| **CI** | `mvn test` + smoke do `ManifestCli` no fixture valido | GitHub Actions em todo push/PR (`.github/workflows/ci.yml`) |
| **Artefato** | Apos testes verdes: `mvn package`, zip jar + fixtures + README, `upload-artifact` | Job `artifact` do mesmo workflow |
| **Release tag** | Em tags `v*` publica zip no GitHub Release | Soft — precisa remote GitHub e `GITHUB_TOKEN` |
| **Deploy cloud API** | App Runner/ECS + RDS + S3 (manual/conta AWS) | Documentado acima; nao automatizado no Actions deste case |

### Conferir Actions

1. Abra o repositorio no GitHub → aba **Actions**.
2. Selecione o workflow **CI** e o run do commit/PR.
3. Confira jobs `test` (unitarios + smoke) e `artifact` (download do zip).

### Tag de versao (apos merge)

```powershell
git tag v1.1.0-manifest
git push origin v1.1.0-manifest
```

O push da tag dispara o job `release` (quando o remote for GitHub).

Equivalente local ao CI:

```powershell
mvn -B test
mvn -q exec:java@manifest-cli "-Dexec.args=data/manifestos/valido.json"
mvn -B package -DskipTests
```

## Banco de Dados Inter (auditoria)

Painel **terminal** de auditoria de entregas/reembolsos (sem web/API propria). Opcao de menu **10**.

### Infra local

```powershell
docker compose up -d
# sobe Postgres (schema API + aud_* + seeds), Redis e MongoDB
```

Fallback sem Docker: `$env:FILAFACIL_DB_URL="memory"` (store em memoria com o mesmo seed).

### Modelo relacional (`aud_*`)

| Tabela | Papel |
|--------|-------|
| `aud_clientes` | clientes |
| `aud_motoristas` | motoristas |
| `aud_entregas` | entregas (FK cliente/motorista) |
| `aud_ocorrencias` | ocorrencias opcionais por entrega |
| `aud_reembolsos` | reembolsos por entrega |

Scripts: `src/main/resources/auditoria/02-auditoria-schema.sql`, `03-auditoria-seed.sql`.

### Indices (justificativa)

| Indice | Campo | Por que |
|--------|-------|---------|
| `idx_aud_entregas_status` | status | filtro frequente na listagem |
| `idx_aud_entregas_data` | data_entrega | filtro por periodo |
| `idx_aud_entregas_cliente` | cliente_id | join/filtro por cliente |
| `idx_aud_entregas_motorista` | motorista_id | join/filtro por motorista |
| `idx_aud_reembolsos_status` | status | contagem/aprovacao |
| `idx_aud_reembolsos_entrega` | entrega_id | join com entregas |

### Joins / paginacao / transacao / cache / NoSQL

- **INNER JOIN** entregas + clientes + motoristas
- **LEFT JOIN** ocorrencias (entrega sem ocorrencia continua na lista)
- **RIGHT JOIN** motoristas (inclui motorista sem entregas) — demo no submenu
- Listagem paginada: pagina + tamanho + total
- Aprovacao de reembolso em transacao (`BEGIN`/`COMMIT`/`ROLLBACK`)
- Redis: chave `filafacil:auditoria:resumo:v1`, TTL 60s, invalidacao na aprovacao
- MongoDB collection `audit_events` (ou memoria se Mongo offline): tipo, usuario, entidade, dados antigos/novos

### Submenu 10

1. Listar entregas paginadas  
2. Demo joins  
3. Aprovar reembolso  
4. Resumo cacheado  
5. Eventos NoSQL  

## Data Engineering — Pipeline batch

Pipeline local em Java (`com.mundodev.filafacil.pipeline`) com etapas **ingest → transform → load**, camadas **bruta / processada / consolidada**, particionamento por `dt` + `runId`, retries e logs.

### Fontes (fixtures classpath → `data/pipeline/input`)

| Fonte | Formato | Conteudo |
|-------|---------|----------|
| coletas | CSV | codigo, cliente, regiao, status, prazos, data |
| ocorrencias | JSON | tipo, descricao, referencia, data, status |
| entregas | Parquet | id, regiao, datas, status, atraso, cliente, unidade |
| clientes/unidades | tabela `pipe_clientes_unidades` (Postgres) ou CSV fallback | cadastro para join |

Fixtures canonicos: `src/main/resources/pipeline/fixtures/`. Parquet gerado via DuckDB JDBC a partir do CSV de entregas.

### Camadas e saidas

```
data/pipeline/
  input/
  bruta/dt=YYYY-MM-DD/run=<id>/
  processada/dt=.../run=.../
  consolidada/dt=.../run=.../   # metricas.json + metricas.csv
  logs/run-<id>.log
```

### Como rodar

```powershell
# Via menu: opcao 11
mvn -q compile exec:java

# Via CLI
mvn -q compile exec:java@pipeline

# Sem Postgres (cadastro/metricas so em arquivo)
$env:FILAFACIL_DB_URL = "memory"
mvn -q compile exec:java@pipeline
```

Com Docker: `docker compose up -d` aplica `04-pipeline-schema.sql` + `05-pipeline-seed.sql` e a carga tenta upsert em `pipe_metricas_diarias`. Se o banco estiver offline, o pipeline segue com fallback CSV/arquivo (documentado no log).

### Metricas

Agregacoes por data, regiao, status e cliente: totais de coletas, entregas, atrasos e ocorrencias. Registros invalidos sao descartados com contagem (sem perda silenciosa).

Env: `FILAFACIL_PIPELINE_ROOT` (default `data/pipeline`), `FILAFACIL_PIPELINE_RETRIES` (default `2`).

## Data Engineering — Pipeline intermediario (CDC)

Evolucao do batch basico em `com.mundodev.filafacil.pipeline.inter`: carga **batch** ou **incremental**, CDC simulado, upsert/dedup, checkpoints por fonte, late arriving, lineage, Parquet particionado na processada e paralelismo entre fontes.

### Fontes (4)

| Fonte | Fixture | Controle | Prioridade |
|-------|---------|----------|------------|
| API coletas (mock) | `fixtures/inter/coletas_api.json` | `updated_at` / `version` | 30 |
| Clientes (relacional/CSV) | `fixtures/inter/clientes.csv` | `updated_at` | 40 |
| Entregas CSV/Parquet | `fixtures/inter/entregas.csv` (+ parquet) | `updated_at` | 20 |
| Motoristas/rotas JSON | `fixtures/inter/motoristas_rotas.json` | `updated_at` / `deleted` | 10 |

### Comportamento

- **Incremental:** le apenas registros com `updated_at` > checkpoint da fonte
- **Dedup/merge:** chave `entity|business_key`; vence maior `updated_at`, depois `version`, depois prioridade da fonte; versao antiga = `SKIP_STALE`
- **Late arriving:** `FILAFACIL_PIPELINE_LATE_POLICY=mark|reject|accept` (default `mark`)
- **Checkpoints:** `data/pipeline/checkpoints/checkpoints.json` (por fonte; so avanca em sucesso)
- **Lineage:** `data/pipeline/lineage/lineage-<runId>.jsonl`
- **Processada:** Parquet particionado por `dt` + `regiao` em `processada/inter/run=<id>/`
- **Schema SQL:** `06-pipeline-inter-schema.sql` (`pipe_checkpoints`, `pipe_lineage`)

### Como rodar

```powershell
# Menu opcao 12
mvn -q compile exec:java

# CLI incremental
mvn -q compile exec:java@pipeline "-Dexec.args=--mode incremental"

# CLI batch inter + reprocessamento de periodo
mvn -q compile exec:java@pipeline "-Dexec.args=--mode batch --reprocess --from 2026-11-01T00:00:00 --to 2026-11-30T23:59:59"

# Uma fonte
mvn -q compile exec:java@pipeline "-Dexec.args=--mode incremental --source entregas"
```

## Design de Software (dominio coletas)

Organizacao **feature-based** em `com.mundodev.filafacil.coletas`:

```
coletas/
  Coleta.java
  ColetaService.java          # coordena; DI apenas (sem new de deps)
  factory/ColetaFactory.java  # Factory: coleta valida + status pendente
  priority/
    PriorizacaoStrategy.java
    PrioridadeManualStrategy.java      # default no Main
    PrioridadePorPacotesStrategy.java  # >=10 alta, >=5 media, senao baixa
  observer/
    ColetaObserver.java
    ColetaEventPublisher.java
    LogOperacionalObserver.java
    ContadorMemoriaObserver.java
  repo/
    ColetaRepository.java
    FileColetaRepository.java
    InMemoryColetaRepository.java      # testes
```

| Pattern | Onde |
|---------|------|
| Factory | `ColetaFactory.criar(...)` usada no cadastro |
| Strategy | `PriorizacaoStrategy` injetada; troque no wiring do `Main` |
| Observer | `ColetaEventPublisher` notifica em criar/atualizar status |
| DI | `Main` monta repository/factory/strategy/publisher/CEP e injeta no service |

Contratos: `ColetaRepository`, `PriorizacaoStrategy`, `ColetaObserver`, `CepApiClient`. Testes em `src/test/java/.../coletas/` usam fakes (sem arquivo real).

## Arquitetura

```
controller.MenuController
  → coletas.ColetaService
      → coletas.repo.ColetaRepository (File | InMemory)
      → coletas.factory.ColetaFactory
      → coletas.priority.PriorizacaoStrategy
      → coletas.observer.ColetaEventPublisher
      → state.AppState
      → integration.CepApiClient (ViaCEP)
  → inference.OperationalTriageService (one-shot)
  → inference.prompt.ConversationOrchestrator (multi-turno)
      → PromptLibrary / FewShotCatalog / PromptComposer
      → ConversationSession + ClarificationEngine
      → GroundingValidator + OrientedRetry
      → FallbackInferenceGateway (Mock | HttpOpenAI-compatible)
  → manifest.ManifestValidationService / ManifestCli
  → pipeline.PipelineModule (menu 11 / PipelineCli)
  → pipeline.inter.PipelineInterModule (menu 12 / --mode)

api.ApiMain (entrypoint separado)
  → AppConfig (env)
  → JdbcAgendamentoRepository (Postgres)
  → AttachmentStorage (local filesystem | S3)
  → AgendamentoService → Javalin routes (/health, /agendamentos)
```

Schema triagem: `src/main/resources/inference/triage-schema.json`  
Schema API: `src/main/resources/api/schema.sql`

## Analise de falhas (exemplo)

Se o golden dataset marcar divergencia em `tipoSolicitacao` ou `prioridadeSugerida`:

1. Conferir se a mensagem do caso cobre as palavras-chave do mock/router.
2. Ajustar heuristica em `MockInferenceGateway` / `ModelRouter` (nao so o expected).
3. Reexecutar `OfflineEvalRunner` e registrar impacto operacional (triagem errada → acao/prioridade incorreta).

## Estrutura

```
src/main/java/com/mundodev/filafacil/
  Main.java
  controller/MenuController.java
  coletas/
    ColetaService.java
    factory/
    priority/
    observer/
    repo/
  state/AppState.java
  integration/...
  inference/
    InferenceFactory.java
    OperationalTriageService.java
    ModelRouter.java
    MockInferenceGateway.java
    HttpOpenAiCompatibleGateway.java
    FallbackInferenceGateway.java
    TriageSchemaValidator.java
    tools/
    eval/OfflineEvalRunner.java
  manifest/
    ManifestCli.java
    ManifestValidationService.java
    ManifestValidator.java
    ManifestReader.java
  api/
    ApiMain.java
    AppConfig.java
    ApiApp.java
    service/AgendamentoService.java
    repo/
    storage/
  pipeline/
    PipelineModule.java / PipelineCli.java / PipelineOrchestrator.java
    stage/ IngestStage TransformStage LoadStage
    io/ ParquetIO FixtureMaterializer SourceReaders
    store/CadastroRepository.java
  model/...
  error/...
src/main/resources/pipeline/
data/pipeline/   # gerado em runtime (gitignore)
docker-compose.yml
.env.example
infra/
.github/workflows/ci.yml
src/test/resources/inference/golden/
src/test/java/.../inference/
src/test/java/.../manifest/
src/test/java/.../api/
src/test/java/.../pipeline/
```
