# Tasks

## 1. Validador de manifesto

- [x] 1.1 Criar pacote `manifest` (record, reader JSON/CSV, validator) e fixtures `data/manifestos/` (valido + invalido); verificar leitura local dos fixtures
- [x] 1.2 Implementar regras (campos obrigatorios, duplicatas, rota/veiculo/responsavel) e testes JUnit cobrindo valido, duplicata e campo vazio; verificar `mvn -q test` passa nos novos testes
- [x] 1.3 Implementar `ValidationReport` (totais, erros, aprovado/reprovado) com saida terminal e opcao de salvar arquivo; verificar teste de status e conteudo do relatorio

## 2. CLI e menu

- [x] 2.1 Criar `ManifestCli` com exit code 0/1 e args de caminho/`--out`; verificar execucao local no manifesto valido (0) e invalido (1)
- [x] 2.2 Integrar opcao de menu para validar manifesto sem quebrar CRUD/inference; verificar app inicia e a opcao imprime relatorio

## 3. Pipeline CI/CD

- [x] 3.1 Adicionar `.github/workflows/ci.yml` (push/PR: setup JDK 17, `mvn test`, smoke do validador no fixture valido); verificar YAML valido e etapas documentadas
- [x] 3.2 Configurar job de artefato compactado (upload-artifact) apos testes verdes e documentar job/release em tags `v*`; verificar workflow gera artefato no desenho do YAML
- [x] 3.3 Atualizar README com CI vs deploy, como rodar localmente, como conferir Actions e procedimento de tag de versao; verificar instrucoes batem com caminhos/comandos reais

## Workflow follow-up

- Criar tag Git de versao do validador apos merge (ex.: `v1.1.0-manifest`) quando houver remote.
- Archive a change (`/opsx-archive`) apos review.
- Opcional: concluir archive pendente de `filafacil-prompt-engineering` se ainda ativo.
