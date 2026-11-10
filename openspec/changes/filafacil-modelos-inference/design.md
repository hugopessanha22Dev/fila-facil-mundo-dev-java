# Design

## Context

Código atual: Java 17+ Maven, terminal com `MenuController` → `ColetaService` → `ColetaFileStore`, ViaCEP async, `AppState`, erros tipados (`EntradaException` / `ArquivoException` / `ApiException`), JUnit. Sem camada de LLM. Ver `proposal.md` — Why.

Restrições do case: ~6h, sem web/banco/framework pesado; validação programática obrigatória; ≥2 perfis de modelo; tools podem ser mockadas.

## Goals / Non-Goals

**Goals:**

- Gateway único de inferência pluggável (HTTP OpenAI-compatible + mock)
- Schema JSON da triagem + validação antes de uso
- Roteamento leve vs detalhado + parâmetros por cenário
- Tools com contrato e implementação mock (opcionalmente lendo coletas em memória/arquivo)
- Golden dataset + runner de eval offline
- Opção de menu para triagem sem quebrar CRUD existente

**Non-Goals:**

- UI web, streaming em tempo real no terminal (opcional futuro), fine-tuning
- Multi-provider complexo além de OpenAI-compatible + mock
- Persistência de histórico de inferências em banco
- Substituir ViaCEP ou o fluxo de cadastro atual

## Decisions

### 1. Pacote `inference` isolado

```
com.mundodev.filafacil.inference
  InferenceGateway (+ MockInferenceGateway / HttpOpenAiCompatibleClient)
  ModelRouter
  TriageSchemaValidator
  TriageResult (record/POJO)
  tools/* (interfaces + mocks)
  eval/OfflineEvalRunner
```

- **Por que:** espelha a regra “100% das chamadas via camada de serviço” e evita acoplar LLM em `ColetaService`.
- **Alternativa:** metódos de IA dentro de `ColetaService` — rejeitada (mistura domínio de coleta com inferência).

### 2. Provedor OpenAI-compatible + dois modelIds

- Env: `FILAFACIL_LLM_API_KEY`, `FILAFACIL_LLM_BASE_URL` (default OpenAI/Groq), `FILAFACIL_LLM_MODEL_LIGHT`, `FILAFACIL_LLM_MODEL_DETAILED`, `FILAFACIL_LLM_MODE=http|mock`.
- HttpClient JDK + Jackson (já no projeto).
- **Por que:** atende “2+ modelos” com uma integração; mock cobre testes/CI sem chave.
- **Alternativa:** SDKs oficiais distintos — custo de deps e tempo fora do escopo ~6h.

### 3. Validação de schema

- Schema JSON versionado em `src/main/resources/inference/triage-schema.json`.
- Validação: checagem estrutural com Jackson (campos obrigatórios/tipos/enums) **ou** lib leve de JSON Schema se necessário no apply.
- Resposta inválida → erro tipado (ex. estender `ApiException` ou `InferenceException`) + possível fallback uma vez; se ainda inválido, rejeitar.

### 4. Roteamento heurístico

- Regras determinísticas locais (palavras-chave de urgência/reclamação, tamanho do texto, menção a “atraso”/“não chegou”) → perfil detalhado; senão leve.
- Parâmetros: crítico → temperature ~0–0.2; resumo/leve → levemente maior.
- **Alternativa:** sempre modelo detalhado — viola meta de custo/latência do case.

### 5. Tools mock

- Interfaces Java + mocks que leem `AppState`/`ColetaService.listar()` quando id existir; `registrarOcorrencia` / `encaminharHumano` apenas retornam confirmação simulada (log).
- Function calling: se o provedor retornar tool calls, o gateway executa mocks e reenvia; se não houver tool calling estável no mock, o prompt pede JSON final direto e as tools são exercitadas no eval unitário.

### 6. Menu e erros

- Nova opção (ex. `6 - Triagem IA`) no `MenuController`: lê texto multilinha simples (até linha vazia), chama router→gateway→validator, imprime JSON/resumo.
- Erros tipados alimentam `AppState.setUltimoErro` como no Módulo 2.

### 7. Eval offline

- Dataset: `src/test/resources/inference/golden/*.json` (input + expected parcial).
- Runner JUnit ou classe main `OfflineEvalRunner` acionável por `mvn -q exec:java -Dexec.mainClass=...` / teste dedicado com `FILAFACIL_LLM_MODE=mock`.
- README: tabela de modelos, como setar env, como rodar eval e seção de análise de falhas.

## Risks / Trade-offs

- [Sem chave de API o aluno não vê chamada real] → Mitigation: modo mock completo + README; HTTP opcional.
- [Function calling inconsistente entre provedores] → Mitigation: priorizar JSON schema na mensagem do sistema; tools testadas unitariamente.
- [Custo/latência variáveis] → Mitigation: perfil leve default; log de custo estimado por tabela local simples (tokens × preço config).
- [Escopo >6h] → Mitigation: mocks first, HTTP thin client, heurística de roteamento simples.

## Migration Plan

1. Adicionar pacote `inference` e recursos de schema/dataset sem alterar menu.
2. Ligar opção de menu e env vars.
3. Rodar testes mock + eval; documentar README.
4. Rollback: remover opção de menu / flag off; CRUD permanece intacto.

## Open Questions

- Qual base URL/modelIds concretos o usuário prefere no apply (OpenAI vs Groq vs outro OpenAI-compatible) — não muda specs; só valores default de env.
