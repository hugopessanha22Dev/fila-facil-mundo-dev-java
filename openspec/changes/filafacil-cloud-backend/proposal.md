# Proposal

## Why

O FilaFácil hoje é um app de **terminal** (coletas JSON, ViaCEP, inference, validação de manifesto e CI), sem API HTTP de agendamento de docas, sem banco gerenciado, sem storage de anexos e sem publicação em cloud com load balancer/auto scaling. O case Mundo Dev **Cloud para Backend** pede exatamente essa lacuna: API de agendamentos publicada na cloud com infra mínima real (~6h), sem frontend nem auth completa.

## What Changes

- Introduzir API HTTP de agendamento de docas: criar, listar, consultar por data, atualizar status e cancelar
- Separar configuração por variáveis de ambiente (porta, banco, storage, região, modo local/produção); zero credenciais no código
- Persistir agendamentos em banco gerenciado (cloud) com modo local equivalente para desenvolvimento
- Armazenar anexos/comprovantes em storage cloud e guardar só a referência no banco
- Expor `/health` e publicar a API atrás de load balancer, com auto scaling documentado (min/max + regra simples)
- Aplicar IAM de permissões mínimas (banco, storage, logs) e logs de produção sem dados sensíveis
- Documentar no README deploy, env vars, banco, storage, LB, auto scaling, IAM e onde consultar logs
- Manter o app terminal existente (menu/CRUD/inference/manifest/CI); a API cloud é um entrypoint/módulo separado
- Premissas: provedor cloud concreto **AWS** (App Runner ou ECS Fargate + ALB + RDS Postgres + S3); sem frontend, sem auth completa, sem K8s obrigatório, sem microsserviços, sem multi-região

## Capabilities

### New Capabilities

- `dock-scheduling-api`: endpoints HTTP de agendamento (criar, listar, por data, atualizar status, cancelar) e contrato de dados (transportadora, placa, doca, status)
- `environment-config`: configuração externa local vs produção via env vars (porta, DB, bucket, região, modo)
- `managed-persistence`: persistência de agendamentos em banco gerenciado com tratamento de falha sem vazar credenciais
- `attachment-storage`: upload/referência de anexos em storage cloud (sem arquivos grandes)
- `cloud-runtime-ops`: health check, deploy publicado, load balancer, auto scaling documentado, IAM mínimo e logs de produção

### Modified Capabilities

- (nenhuma main spec arquivada obrigatória; o terminal existente permanece e não muda de requisitos)

## Impact

- Novo módulo/pacote HTTP (ex.: `com.mundodev.filafacil.cloud` ou `api`) com main separado do terminal
- Dependências: servidor HTTP leve ou Spring Boot mínimo, driver JDBC/Postgres, SDK S3 (ou cliente S3-compatible)
- Infra versionada ou documentada (ex.: `infra/` com políticas IAM, parâmetros de scaling, compose local)
- `.env.example` / README de cloud; secrets só via env/plataforma
- CI existente pode ganhar smoke de `/health` local; deploy real depende de conta AWS do aluno
- Referência: PDF *Cloud para Backend – FilaFácil Logística* (Escola Mundo Dev)
