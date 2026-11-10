# Tasks

## 1. Biblioteca de prompts e few-shot

- [x] 1.1 Criar resources `inference/prompts/` com os 5 prompts (classify, extract, summarize, internal-reply, transform) no formato estruturado; verificar carregamento via classpath
- [x] 1.2 Implementar `PromptLibrary` que carrega e expoe as definicoes; verificar teste unitario cobre as 5 tarefas e campos obrigatorios da estrutura
- [x] 1.3 Criar `inference/fewshot/catalog.json` com exemplos positivos, negativos, contraexemplos e escalacao (dados ficticios); verificar teste carrega os 4 tipos
- [x] 1.4 Implementar `FewShotCatalog` e injecao de exemplos no prompt composto; verificar teste que a mensagem enviada ao gateway inclui few-shot

## 2. Memoria conversacional

- [x] 2.1 Implementar `ConversationSession` (intencao, campos, perguntasFeitas, flags desconhecido); verificar testes de update sem perder campos anteriores
- [x] 2.2 Implementar deteccao de lacunas e geracao de pergunta de clarificacao sem repetir perguntas respondidas; verificar cenarios unitarios
- [x] 2.3 Implementar `ConversationOrchestrator` (recepcao → lacunas → conclusao estruturada); verificar fluxo mock de 2+ turnos ate JSON final

## 3. Grounding e retry

- [x] 3.1 Implementar `GroundingValidator` (rubricas de formato/coerencia/anti-invencao e origem fato|inferencia|ausente); verificar rejeicao de status inventado
- [x] 3.2 Implementar retry orientado ao erro com limite maximo; verificar teste de retry capped e escalacao humana ao esgotar
- [x] 3.3 Refatorar gateway HTTP/mock para aceitar system prompt dinamico (biblioteca + few-shot + historico); verificar one-shot existente ainda funciona

## 4. Menu e documentacao

- [x] 4.1 Adicionar opcao de menu “Atendimento conversacional” e wiring no `Main`; verificar app inicia em mock e triagem one-shot (opcao 6) permanece
- [x] 4.2 Documentar no README a biblioteca de prompts, few-shot, fluxo conversacional e regras de grounding; verificar instrucoes batem com resources/comandos reais
- [x] 4.3 Adicionar testes de integracao leves (golden conversacional mock) e rodar `mvn clean test`; verificar suite verde

## Workflow follow-up

- Archive a change apos review (`/opsx-archive`) quando a implementacao estiver estavel.
- Opcional: arquivar tambem `filafacil-modelos-inference` se ainda nao arquivada.
