# Proposal

## Why

O FilaFácil já cobre coletas, inferência e prompts em terminal, mas a validação de manifestos de carga ainda seria manual e o repositório não tem pipeline CI/CD (sem workflow, sem artefato automatizado, sem tag/release documentada). O case Mundo Dev **DevOps Fundamentals** pede um validador local simples + integração contínua + deploy automatizado leve — evolução natural sem web, banco ou cloud complexa.

## What Changes

- Adicionar módulo terminal de validação de manifesto (JSON ou CSV) com campos obrigatórios: pacote, rota, veículo, responsável, status
- Detectar pacotes duplicados, rotas vazias e registros sem veículo/responsável
- Gerar relatório (totais, lista de erros, status aprovado/reprovado) no terminal e/ou arquivo local
- Configurar pipeline GitHub Actions em push/PR: instalar dependências, compilar, testes, validar execução do validador
- Após sucesso, gerar artefato compactado (e, quando possível, release/tag documentada)
- Documentar no README o fluxo CI vs deploy, como rodar localmente e como conferir a automação
- Manter o menu/CRUD/inference existentes; o validador entra como opção/comando separado sem quebrar o restante
- Premissas: GitHub Actions; manifesto de exemplo em `data/`; sem Kubernetes, sem multi-ambiente, sem autenticação

## Capabilities

### New Capabilities

- `manifest-validation`: leitura e validação de manifesto local (JSON/CSV) com regras de campos, duplicatas e rotas problemáticas
- `validation-report`: relatório estruturado com totais, erros e status final aprovado/reprovado
- `ci-pipeline`: integração contínua automática em push/PR (deps, build, testes, validação)
- `automated-artifact-deploy`: entrega automatizada simples (artefato compactado / release) após pipeline verde

### Modified Capabilities

- (nenhuma main spec arquivada obrigatória para este escopo; o app existente permanece e é estendido)

## Impact

- Novo pacote `com.mundodev.filafacil.manifest` (modelo, reader, validator, report, CLI/main entry)
- `.github/workflows/ci.yml` (e job de artefato/release simples)
- Fixtures `data/manifestos/` (JSON/CSV válido e inválido)
- `pom.xml`: possível perfil/exec para validador; packaging zip via plugin ou script no workflow
- `MenuController` / README: opção ou comando para validar manifesto + seção CI/CD
- Continua terminal + arquivo local; sem web, MySQL, K8s ou cloud produtiva
- Referência: PDF *DevOps Fundamentals – FilaFácil Logística* (Escola Mundo Dev)
