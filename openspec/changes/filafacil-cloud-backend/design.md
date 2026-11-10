# Design

## Context

Codigo atual: Maven Java 17+, terminal FilaFacil (coletas JSON, ViaCEP, inference, manifesto, GitHub Actions CI). Observado: sem HTTP server, sem `/health`, sem JDBC/banco gerenciado, sem storage cloud, sem IaC/deploy. Ver `proposal.md` — Why.

Restricoes do case: ~6h, sem frontend, sem auth completa, sem microsservicos, sem K8s obrigatorio, sem multi-regiao, sem fila/mensageria, sem upload grande. Provedor concreto escolhido: **AWS**.

## Goals / Non-Goals

**Goals:**

- API HTTP unica de agendamentos + `/health`, entrypoint separado do terminal
- Env vars local vs producao; Postgres gerenciado (RDS) + S3; modo local com Postgres via Docker Compose (ou H2 so se Compose indisponivel — preferir Postgres local)
- Artefatos de infra/documentacao: App Runner **ou** ECS Fargate + ALB, auto scaling, IAM policy minima, logs CloudWatch
- README completo dos 8 requisitos obrigatorios do case
- Nao quebrar menu/CRUD/inference/manifest/CI existentes

**Non-Goals:**

- Frontend, OAuth/JWT completo, multi-tenant, microsservicos
- Kubernetes obrigatorio, multi-AZ avancado, tuning de banco
- Substituir o dominio de coletas/terminal pela API de docas

## Decisions

### 1. Modulo HTTP separado (Javalin ou JDK HttpServer + JSON)

- Pacote `com.mundodev.filafacil.api` (ou `cloud`) com `ApiMain`
- Rotas REST JSON para agendamentos + `GET /health`
- **Por que:** o `pom` atual nao e Spring Boot; adicionar framework minimo (preferencia **Javalin**) evita reescrever o terminal e cabe em ~6h
- **Alternativa:** Spring Boot monorepo — rejeitada por custo de migracao no prazo
- **Alternativa:** so documentar cloud sem API no repo — rejeitada (case exige endpoints)

Endpoints planejados:

| Metodo | Rota | Acao |
|--------|------|------|
| POST | `/agendamentos` | criar |
| GET | `/agendamentos` | listar |
| GET | `/agendamentos?data=YYYY-MM-DD` | filtrar por data |
| PATCH | `/agendamentos/{id}/status` | atualizar status |
| POST | `/agendamentos/{id}/cancelar` | cancelar |
| POST | `/agendamentos/{id}/anexo` | upload pequeno (multipart ou base64 limitado) |
| GET | `/health` | health |

### 2. Config por env (`AppConfig`)

Variaveis (nomes finais no README):

- `FILAFACIL_API_MODE` = `local` | `prod`
- `FILAFACIL_API_PORT`
- `FILAFACIL_DB_URL` / `FILAFACIL_DB_USER` / `FILAFACIL_DB_PASSWORD`
- `FILAFACIL_S3_BUCKET` / `FILAFACIL_AWS_REGION`
- Credenciais AWS: role IAM na cloud; local via env padrao AWS SDK (nunca commitadas)

`.env.example` versionado; `.env` no `.gitignore`.

### 3. Persistencia Postgres + schema simples

Tabela `agendamentos`: id, transportadora, placa, doca, data_hora, status, anexo_ref, created_at, updated_at.

- Local: `docker-compose.yml` com Postgres
- Prod: RDS Postgres; security group/rede permitindo origem do servico da API
- Falhas: mapear para HTTP 503/500 com mensagem generica; log interno sem senha

### 4. Storage S3

- Upload gera chave `agendamentos/{id}/{filename}`
- Banco guarda so `anexo_ref` (s3://bucket/key ou key)
- Limite de tamanho baixo (ex.: 1–2 MB) alinhado a restricao "sem arquivos grandes"
- Local: LocalStack **opcional** ou filesystem `data/anexos/` quando `MODE=local` (documentar o desvio; em prod sempre S3)

### 5. Deploy AWS (caminho padrao: App Runner + RDS + S3 + ALB se necessario)

Para caber em 6h:

1. Empacotar jar da API (`ApiMain`)
2. Publicar em **AWS App Runner** (ou ECS Fargate se App Runner nao couber no caso do aluno) com env vars da plataforma
3. Se o servico ja expoe URL HTTPS, documentar como "load balancer gerenciado do servico"; se usar ECS, ALB explicito
4. Auto scaling: min/max no servico (ex.: 1–3) + metrica CPU ou concurrent requests
5. IAM task/instance role: `rds-db:connect` ou credenciais via Secrets Manager + `s3:PutObject/GetObject` no bucket + CloudWatch Logs
6. Pasta `infra/`: policy IAM JSON de exemplo + parametros de scaling + checklist de deploy (Terraform completo e opcional/nao bloqueante)

### 6. Logs

- SLF4J + Logback (ou java.util.logging) → stdout (App Runner/CloudWatch coleta)
- Eventos: startup, request method/path (sem body com PII), erros DB/S3, health
- Nunca logar password, Authorization, connection string com senha

### 7. Testes e CI

- JUnit: validacao de payload, repositorio com Testcontainers Postgres **se** viavel; senao H2 so nos testes unitarios de regra + testes de integracao manuais documentados
- CI atual: adicionar job/step opcional `mvn test` ja existente; smoke local `GET /health` apos subir API em background **somente se** estavel no Actions — nao falhar o CI do terminal se a API precisar de Postgres (preferir testes unitarios sem rede)

### 8. Convivencia com o terminal

- `Main` (terminal) intacto; `ApiMain` via `exec:java@api`
- Menu terminal **nao** precisa listar agendamentos cloud (escopo e API-first); opcional link no README

## Risks / Trade-offs

- [Conta AWS / custos] → Mitigation: README com free tier / limpeza; modo local completo sem cloud
- [App Runner vs ALB explicito] → Mitigation: documentar LB gerenciado do servico; se avaliador exigir ALB visual, fallback ECS+ALB no README
- [LocalStack complexidade] → Mitigation: filesystem local para anexos em `MODE=local`
- [Escopo >6h se misturar Spring + Terraform full] → Mitigation: Javalin + Compose + infra minima documentada
- [Segredo vazado em log] → Mitigation: checklist e testes de mensagem de erro

## Migration Plan

1. Adicionar modulo API + schema + testes locais (Compose) sem alterar terminal
2. Ligar S3/filesystem e env config
3. Documentar/deploy AWS + IAM + scaling + logs
4. Atualizar README; smoke `/health` na URL publica
5. Rollback: desligar servico cloud; terminal e CI manifesto seguem intactos

## Open Questions

- Nenhuma que bloqueie o planejamento: AWS + Javalin + Postgres + S3 e o default; se a conta do aluno for GCP/Azure, o design de capacidades permanece e so a pasta `infra/` / README mudam na aplicacao futura.
