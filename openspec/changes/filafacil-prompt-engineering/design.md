# Design

## Context

Estado atual: `filafacil-modelos-inference` entregou gateway (mock/HTTP), roteamento leve/detalhado, schema de triagem one-shot, tools mock e eval offline. O HTTP usa um `SYSTEM_PROMPT` monolítico; o mock classifica por heurística. Menu opção 6 = triagem one-shot. Ver `proposal.md` — Why.

Restrições do case: ~6h, sem fine-tuning, sem UI web, sem dados sensíveis reais; confiabilidade > velocidade.

## Goals / Non-Goals

**Goals:**

- Biblioteca de prompts versionada por tarefa + few-shot catalog
- Sessão conversacional com memória de campos e clarificação incremental
- Grounding/rubricas + retry orientado limitado
- Integrar ao gateway existente (prompt composto: system da tarefa + few-shots + histórico)
- Nova opção de menu conversacional; manter triagem one-shot

**Non-Goals:**

- Treinar/fine-tune modelo; UI gráfica; vector DB / RAG externo
- Substituir ViaCEP/CRUD; persistir histórico de conversas em banco
- Multi-agente complexo além do fluxo de clarificação

## Decisions

### 1. Resources versionados para prompts e few-shots

```
src/main/resources/inference/prompts/
  classify.json
  extract.json
  summarize.json
  internal-reply.json
  transform.json
src/main/resources/inference/fewshot/
  catalog.json
```

Cada prompt JSON: `{ id, objective, context, inputExpected, constraints[], qualityCriteria[], outputFormat, systemTemplate }`.

- **Por que:** editável sem recompilar lógica; auditável no Git.
- **Alternativa:** strings Java hardcoded — pior para o case de prompt engineering.

### 2. Pacote `inference.prompt`

```
PromptDefinition / PromptLibrary
FewShotExample / FewShotCatalog
ConversationSession (campos, intenção, turnos, perguntasFeitas)
ConversationOrchestrator (recepção → lacunas → ambiguidade → conclusão)
GroundingValidator + OrientedRetry
```

`OperationalTriageService` continua one-shot; `ConversationOrchestrator` é o caminho novo.

### 3. Composição da chamada ao modelo

1. Selecionar `PromptDefinition` da tarefa (ou pipeline: classify → extract → … conforme fase).
2. Anexar few-shots filtrados por tarefa/tipo.
3. Anexar resumo da `ConversationSession` (campos preenchidos + ausentes).
4. Gateway completa; `GroundingValidator` aplica rubricas + schema.
5. Se falhar: retry ≤ N com mensagem de correção específica; senão escalar humano.

Mock: `MockInferenceGateway` (ou decorator) passa a respeitar fase/sessão (perguntar clarificação se faltam campos; concluir só quando completo).

### 4. Memória de contexto in-memory por sessão de menu

- Uma `ConversationSession` ativa enquanto o operador permanece na opção conversacional.
- Campos: `codigoEntrega`, `cliente`, `endereco`, `prazo`, `status`, `tipo`, `prioridade`, `proximaAcao`, flags `desconhecido.*` + justificativa.
- Ao sair da opção / “encerrar caso”, limpa a sessão.
- **Alternativa:** persistir em arquivo — fora do escopo ~6h.

### 5. Menu

- Manter `6 - Triagem IA` (one-shot).
- Adicionar `7 - Atendimento conversacional`: loop de turnos até conclusão ou cancelamento; imprime perguntas de clarificação ou JSON final validado.

### 6. Grounding no schema de saída conversacional

Estender/variante do JSON com:
- `campos[].valor`, `campos[].origem` (`fato` | `inferencia` | `ausente`)
- `evidencia` opcional (trecho da mensagem)
- Reusar validador estrutural + regras: se `origem=fato`, valor deve aparecer (normalizado) na entrada/histórico; se inventado → falha.

### 7. Testes

- Unit: library carrega 5 prompts; catalog 4 tipos; sessão não re-pergunta; grounding rejeita status inventado; retry capped.
- Golden leve de conversação (2–3 turnos) em mock.

## Risks / Trade-offs

- [HTTP real ainda depende de chave] → Mitigation: mock cobre acceptance; README documenta modo http.
- [Pipeline de 5 prompts por turno estoura custo/latência] → Mitigation: no apply, usar prompt composto por fase (1 chamada/turno) em vez de 5 sequenciais.
- [Heurística mock ≠ LLM real] → Mitigation: mesmos contratos de saída; eval foca schema/grounding/sessão.

## Migration Plan

1. Adicionar resources + classes prompt sem alterar menu.
2. Ligar orchestrator e opção 7; refatorar gateway para aceitar system prompt dinâmico.
3. Testes + README (biblioteca, few-shot, fluxo conversacional).
4. Rollback: remover opção 7; one-shot permanece.

## Open Questions

- Nenhuma que bloqueie specs/tasks; provedor LLM permanece o de `FILAFACIL_LLM_*`.
