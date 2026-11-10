# Tasks

## 1. Fundacao API e configuracao

- [x] 1.1 Criar pacote `com.mundodev.filafacil.api` com `ApiMain`, `AppConfig` (env vars) e `.env.example` + ignore de `.env`; verificar que a API inicia em modo local lendo porta/modo das env vars sem credenciais no codigo
- [x] 1.2 Adicionar dependencia HTTP leve (Javalin) e `GET /health`; verificar resposta de sucesso local e execucao via `exec:java@api`
- [x] 1.3 Documentar no README a tabela de env vars (local vs prod); verificar que os nomes batem com `AppConfig` e `.env.example`

## 2. Persistencia e endpoints de agendamento

- [x] 2.1 Adicionar `docker-compose.yml` Postgres + schema/migration SQL da tabela `agendamentos`; verificar container sobe e a API conecta com URL via env
- [x] 2.2 Implementar repositorio JDBC e endpoints criar/listar/filtrar por data/atualizar status/cancelar; verificar testes JUnit (validacao + regras) e smoke HTTP local nos caminhos principais
- [x] 2.3 Tratar falha de banco com HTTP de erro generico e log sem credenciais; verificar teste ou assert de mensagem que nao expoe senha/connection string sensivel

## 3. Storage de anexos

- [x] 3.1 Implementar storage local (filesystem) em `MODE=local` e adapter S3 em `MODE=prod` com limite de tamanho; verificar upload de anexo pequeno e `anexo_ref` gravado no banco
- [x] 3.2 Documentar bucket/regiao e fluxo de referencia no README; verificar instrucoes coerentes com as env vars reais

## 4. Cloud ops (IAM, LB, scaling, logs, deploy)

- [x] 4.1 Adicionar pasta `infra/` com policy IAM minima de exemplo (RDS/S3/logs) e parametros de auto scaling (min/max + metrica); verificar artefatos versionados e justificativa de permissoes no README
- [x] 4.2 Documentar procedimento de deploy AWS (App Runner ou ECS+ALB), load balancer, CloudWatch logs e checklist de `/health` publico; verificar README cobre os 8 requisitos do case (deploy, banco, storage, LB, scaling, IAM, logs, env)
- [x] 4.3 Garantir logs de startup/request/erro/health sem PII/credenciais e que o terminal existente (`Main`/menu/CI manifesto) continua intacto; verificar `mvn test` da suite atual passa e a API sobe sem alterar o fluxo terminal

## Workflow follow-up

- Deploy real na conta AWS do aluno e validacao do `/health` publico (requer credenciais fora do repo).
- Com Docker Desktop ativo: `docker compose up -d` e API com `FILAFACIL_DB_URL` Postgres (smoke alem do modo `memory`).
- Archive a change (`/opsx-archive`) apos review.
- Opcional: archive pendente de `filafacil-devops-fundamentals` e `filafacil-prompt-engineering` se ainda ativos.
