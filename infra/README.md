# Infra FilaFacil API (AWS)

## Artefatos

| Arquivo | Uso |
|---------|-----|
| `iam-api-role-policy.json` | Policy IAM minima (Secrets Manager DB, S3 anexos, CloudWatch Logs) |
| `auto-scaling.json` | min/max e metrica de escalonamento |

Substitua `REGION`, `ACCOUNT_ID` e `FILAFACIL_BUCKET` antes de aplicar.

## Justificativa IAM

- Sem `AdministratorAccess` ou wildcards amplos em `*` de recursos de conta
- Banco: leitura do secret de conexao (senha fora do codigo)
- Storage: apenas `PutObject`/`GetObject` no prefixo `agendamentos/`
- Logs: criar streams e publicar eventos no log group da API

## Deploy resumido

Ver secao **Cloud Backend** no README raiz do projeto.
