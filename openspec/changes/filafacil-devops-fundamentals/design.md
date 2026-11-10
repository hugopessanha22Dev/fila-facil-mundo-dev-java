# Design

## Context

Codigo atual: Java 17+ Maven, terminal FilaFacil (coletas JSON, ViaCEP, inference, prompts), JUnit local. Observado: sem `.github/workflows`, sem Dockerfile, sem validador de manifesto. Ver `proposal.md` — Why.

Restricoes do case: ~6h, terminal only, arquivo local, sem web/DB/K8s/cloud produtiva; foco em CI/CD simples.

## Goals / Non-Goals

**Goals:**

- Validador de manifesto JSON/CSV + relatorio aprovado/reprovado
- GitHub Actions em push/PR (deps, test, validate fixture)
- Artefato compactado apos sucesso; documentar tag/release
- README com fluxo CI vs deploy
- Nao quebrar menu/CRUD/inference existentes

**Non-Goals:**

- Kubernetes, multi-ambiente, Terraform, cloud deploy real
- API/web/auth para o validador
- Substituir o dominio de coletas pelo de manifesto

## Decisions

### 1. Pacote `manifest` isolado

```
com.mundodev.filafacil.manifest
  ManifestRecord
  ManifestReader (json/csv)
  ManifestValidator
  ValidationReport / ValidationError
  ManifestValidationService
  ManifestCli (main entry opcional)
```

- Expor no menu: opcao `8 - Validar manifesto` (caminho do arquivo + opcional salvar relatorio).
- Tambem `exec-maven-plugin` second main ou args: `ManifestCli` para o CI (`java -cp ... ManifestCli data/manifestos/exemplo.json`).
- **Por que:** mantem responsabilidade unica e permite CI chamar o validador headless.
- **Alternativa:** misturar regras no `ColetaService` — rejeitada (dominios diferentes).

### 2. Formato do manifesto

JSON preferencial (lista de objetos); CSV com cabecalho `pacote,rota,veiculo,responsavel,status`.

Fixtures:
- `data/manifestos/valido.json`
- `data/manifestos/invalido.json` (duplicata + campos vazios)
- espelhos CSV opcionais

### 3. Regras de validacao

1. Campos obrigatorios nao-blank
2. Pacote unico no arquivo
3. Rota nao vazia
4. Veiculo e responsavel presentes

Exit code: `0` se aprovado, `1` se reprovado (util para CI). Relatorio em stdout; flag `--out path` grava arquivo.

### 4. CI: GitHub Actions

`.github/workflows/ci.yml`:
- `on: [push, pull_request]`
- JDK 17, `mvn -B test`
- step: `mvn -q exec:java -Dexec.mainClass=...ManifestCli -Dexec.args=data/manifestos/valido.json` (espera exit 0)
- step opcional: invalido deve falhar (exit 1) em job de smoke negativo ou assert no teste JUnit (preferir JUnit para nao falhar o job inteiro)

Job `artifact` (mesmo workflow, needs: test):
- `mvn -B package`
- compactar jar + fixtures + README snippet em `dist/filafacil-manifest-validator.zip`
- `actions/upload-artifact`

Job `release` (somente tags `v*`): criar GitHub Release com o zip (soft — documentar se `GITHUB_TOKEN` disponivel).

### 5. Versionamento

- Documentar no README criar tag `v1.0.0-manifest` (ou similar) apos merge.
- Tasks de apply incluem instrucao de tag; a criacao remota depende do usuario ter remote/push — nao bloquear se offline.

### 6. Testes

JUnit para reader/validator/report (valido, duplicata, campo vazio, CSV). CI garante suite verde.

## Risks / Trade-offs

- [Actions nao roda sem remote GitHub] → Mitigation: workflow versionado + passos locais equivalentes no README (`mvn test` + CLI).
- [Release job precisa permissoes] → Mitigation: upload-artifact sempre; release so em tags com `contents: write`.
- [Escopo >6h se misturar com inference] → Mitigation: pacote mínimo, fixtures pequenas, um workflow.

## Migration Plan

1. Adicionar pacote + fixtures + testes sem alterar menu.
2. Ligar opcao de menu/CLI e exit codes.
3. Adicionar workflow + documentacao README.
4. Rollback: remover workflow/opcao; app principal intacta.

## Open Questions

- Nenhuma que bloqueie: GitHub Actions e o default; se o remote for outro host, o YAML ainda serve como referencia.
