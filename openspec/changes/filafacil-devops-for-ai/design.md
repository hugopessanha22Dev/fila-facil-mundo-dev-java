# Design

## Context

Hoje o GitHub Actions roda `mvn test`, o smoke do manifesto e um zip de release. A avaliacao offline existente le so `inference/golden` e o teste fixa 3 casos. O assistente cloud ja tem fila, vetor, quotas e redacao de segredo, mas nao ha gate que impeça promover outra versao de prompt ou embedding. Ver `proposal.md` — Why. Requisitos: specs deste change.

Prazo de cerca de 6 horas. A esteira do CI nao pode depender de chave paga nem de conta cloud.

## Goals / Non-Goals

**Goals:**

- Um comando local e um passo no workflow atual que reprova o job se a avaliacao nao passar
- Promocao e rollback simulados em pasta, com a combinacao de versoes explicita
- Golden dataset proprio, sem alterar a contagem dos 3 casos do eval antigo

**Non-Goals:**

- Interface web, treino, fine-tuning, MLOps completo, deploy em servidor pago
- Trocar o job de manifesto ou o contrato JSON do menu de triagem
- Chamar Groq ou OpenAI dentro do CI

## Decisions

### 1. CLI `AiDeliveryMain` via `exec:java@ai-delivery`

Comandos: `evaluate`, `promote homolog|prod`, `rollback homolog|prod`, `status`.

- **Por que:** a esteira precisa de um exit code que o GitHub Actions entende, separado do menu
- **Alternativa:** so documentar o gate — rejeitada, porque o case exige bloqueio automatico
- **Alternativa:** reescrever o workflow inteiro — rejeitada; o job de manifesto continua

O passo novo entra no job `test`, depois de `mvn test`. Exit diferente de zero falha o push. O job `artifact` existente nao e substituido. O relatorio JSON da avaliacao pode ser anexado como artefato extra, sem tirar o zip do manifesto.

### 2. Manifesto de combinacao

Arquivo versionado `src/main/resources/aidevops/release-combo.json` com `promptVersion`, `datasetVersion`, `embeddingVersion`, `schemaVersion`, `modelConfigVersion`.

Cada `evaluate`, `promote` e `rollback` grava essa combinacao no relatorio e no ponteiro do ambiente (`data/ai-delivery/{env}/active.json`), mais `previous.json` antes de substituir.

- **Por que:** rollback vira troca de ponteiro, reproduzivel em teste sem Git
- **Alternativa:** tag Git como unica fonte — rejeitada para o teste local, que precisa simular o ambiente sem push

### 3. Golden dataset separado e gateway mock

Casos em `src/main/resources/aidevops/golden/`, com consulta, ocorrencia e reclamacao, saida esperada e texto ficticio.

A avaliacao usa `MockInferenceGateway` sempre no CI. Um teste injeta um gateway que devolve texto livre ou schema invalido para provar o bloqueio.

- **Por que:** a pasta `inference/golden` hoje tem 3 arquivos e o teste de eval exige total 3. Colocar casos novos ali quebraria o modulo anterior
- **Alternativa:** chamar a Groq no CI — rejeitada pelo custo, pela rede e pelo pedido de caminho gratuito

### 4. Schema proprio da esteira

Validador pequeno, em Jackson, para o objeto com `tipoSolicitacao`, `resumo`, `proximaAcao` e `prioridade`. Nao reutiliza o campo com grafia antiga do menu.

- **Por que:** o gate novo nao pode depender de um nome de campo que o modelo grande ja omitiu
- **Alternativa:** biblioteca de JSON Schema — rejeitada para nao aumentar dependencia no prazo

### 5. Gates e regressao

`aidevops/quality-gates.json` define minimos de JSON valido, schema e acuracia, teto de custo e de latencia, e a queda maxima de acuracia/extracao contra `baseline.json`.

Latencia acima da linha de base, mas ainda abaixo do teto, nao reprova. Queda de acuracia alem do limite reprova.

O relatorio lista as nove metricas: acuracia, extracao, JSON valido, schema, resumo, custo, latencia, regressao e falhas por categoria.

- **Por que:** o case pede equilibrio entre qualidade e variacao irrelevante; no mock a latencia e ruido
- **Alternativa:** um unico percentual geral — rejeitada, porque as nove metricas sao criterio explicito

### 6. Guardrails por ambiente

`aidevops/model-config.json` traz, para dev, homologacao e producao simulada: modelo primario, modelo de fallback, maximo de retries e rate limit. Producao tem retries e rate limit menores ou iguais aos de dev. Nao ha chave no arquivo.

Fluxo de uma saida invalida: repetir ate o teto, chamar o fallback uma vez, marcar intervencao humana se ainda falhar. Entrada sem texto nao chama modelo.

O trace passa pelo `Redactor` ja existente em `cloudai` antes de gravar.

- **Por que:** reaproveita a mascara de `gsk_`, `sk-` e `AQ.` em vez de criar outra
- **Alternativa:** copiar a regex — rejeitada por divergir no futuro

### 7. Lint de prompt

O lint da esteira confere se o prompt da versao aponta para o schema, se nao contem segredo e se o dataset referencia so familias conhecidas. O lint de codigo continua sendo compilacao mais `mvn test`.

- **Por que:** o case fala em lint de artefato de IA, nao em adotar Checkstyle neste prazo
- **Alternativa:** plugin novo de estilo — rejeitada como escopo de MLOps/ferramenta fora do case

## Risks / Trade-offs

- [Mock nao mede qualidade de um modelo real] → o gate prova regressao de contrato e de dataset; ensaio com Groq fica fora do CI, opcional e gratuito
- [Promocao em pasta nao e um servidor] → o ponteiro e o exit code sao o comportamento cobrado; o workflow bloqueia o merge na pratica
- [Grafico de pesos do PDF nao saiu legivel] → avaliacao offline e o passo de CI recebem os testes de bloqueio, como o texto pede

## Migration Plan

1. Acrescentar recursos, CLI e o passo no workflow sem mudar o eval de 3 casos nem o menu
2. `mvn test` cobre evaluate aprovado, evaluate reprovado e rollback
3. Rollback de codigo: remover o passo do workflow e o pacote. Os ponteiros em `data/ai-delivery/` sao locais

## Open Questions

Nenhuma que mude spec, abordagem ou tarefas. Os limiares numericos iniciais ficam no JSON de gates e podem ser ajustados sem mudar o fluxo.
