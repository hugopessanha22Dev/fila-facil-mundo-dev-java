# Tasks

## 1. Combinacao versionada

- [x] 1.1 Criar `src/main/resources/aidevops/` com `release-combo.json` (prompt, dataset, embedding, schema, model config), `model-config.json` por dev/homolog/prod (primario, fallback, retries, rate limit, sem chave) e `quality-gates.json` (minimos de JSON, schema e acuracia, tetos de custo e latencia, queda maxima); verificar que prod tem retries e rate limit menores ou iguais aos de dev e que nenhum arquivo contem chave
- [x] 1.2 Criar o golden dataset em `aidevops/golden/` com consulta, ocorrencia e reclamacao ficticias e saida esperada, mais `baseline.json`; verificar teste de carga das tres familias e que `inference/golden` continua com 3 casos

## 2. Gates e trace

- [x] 2.1 Validar prompt (aponta para o schema e nao tem segredo) e saida (JSON com `tipoSolicitacao`, `resumo`, `proximaAcao`, `prioridade`); recusar texto vazio sem chamar modelo e texto livre; verificar testes de prompt valido, prompt com `gsk_`, JSON aderente e texto livre
- [x] 2.2 Calcular as nove metricas (acuracia, extracao, JSON valido, schema, resumo, custo, latencia, regressao, falhas por categoria) contra o golden e os gates, sem reprovar so por latencia de mock abaixo do teto; verificar teste de avaliacao aprovada no mock e de regressao de acuracia reprovada
- [x] 2.3 Gravar trace por caso com versao, modelo, latencia, custo, tokens, retries, fallback e erro, passando pelo `Redactor` de `cloudai`; verificar teste em que uma chave na resposta sai mascarada

## 3. Promocao, rollback e guardrails

- [x] 3.1 Implementar `evaluate`, `promote` e `rollback` gravando a combinacao em `data/ai-delivery/{env}/active.json` e `previous.json`; recusar promote sem avaliacao aprovada da mesma combinacao e restaurar a anterior no rollback; verificar testes de promote bloqueado, promote aceito e rollback
- [x] 3.2 Aplicar retry ate o teto do ambiente, uma chamada de fallback e marcacao de intervencao humana se a saida continuar invalida; verificar teste com gateway que falha o schema no primario e acerta no fallback, e outro em que os dois falham

## 4. CLI, CI e documentacao

- [x] 4.1 Expor `AiDeliveryMain` em `exec:java@ai-delivery` com `evaluate`, `promote homolog|prod`, `rollback homolog|prod` e `status`, exit code diferente de zero quando o gate falha; verificar execucao local com mock e que o menu 0–12 nao ganha opcao
- [x] 4.2 Acrescentar o passo `evaluate` no job `test` do GitHub Actions, depois de `mvn test`, sem remover o smoke do manifesto nem o zip de release; verificar no YAML que o passo existe e que falha de exit code reprova o job
- [x] 4.3 Documentar no README o comando, o que cada gate bloqueia e que o CI usa mock sem chave; verificar que o comando do README bate com o entrypoint e que `mvn test` permanece verde, inclusive o eval antigo de 3 casos

## Workflow follow-up

- Ensaio opcional com a Groq ja configurada, fora do `mvn test`.
- Arquivar o change (`/opsx-archive`) depois da revisao.
