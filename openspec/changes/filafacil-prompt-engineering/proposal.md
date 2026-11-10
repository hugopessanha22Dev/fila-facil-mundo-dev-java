# Proposal

## Why

A camada de inference do FilaFácil já classifica mensagens em one-shot com um `SYSTEM_PROMPT` fixo e schema JSON, mas ainda não padroniza uma biblioteca de prompts por tarefa, few-shot, memória conversacional nem grounding anti-alucinação. O case Mundo Dev **Prompt Engineering & Interaction** exige esse kit operacional para evitar saídas inconsistentes e inventadas no atendimento.

## What Changes

- Introduzir biblioteca versionada de prompts estruturados (objetivo, contexto, restrições, formato de saída, critérios de qualidade) para: classificação, extração, sumarização, resposta interna e transformação de texto
- Incluir few-shot (exemplos positivos, negativos, contraexemplos e escalação humana) sem dados sensíveis reais
- Evoluir a triagem de one-shot para fluxo conversacional com memória de contexto: clarificar só campos ausentes, sem repetir perguntas já respondidas
- Validar saídas por rubricas (formato, coerência, sem invenção) com retry orientado ao erro específico; diferenciar fato / inferência / ausente
- Substituir o system prompt monolítico do gateway HTTP pelo prompt da tarefa selecionada + few-shot + histórico da sessão
- Expor no menu um modo “Atendimento conversacional” (além da triagem one-shot existente)
- Manter modo mock para testes/CI sem chave; exemplos e prompts em resources versionados

## Capabilities

### New Capabilities

- `prompt-library`: prompts reutilizáveis por tarefa operacional com estrutura padronizada e saída validável
- `few-shot-catalog`: catálogo de exemplos positivos/negativos/contraexemplos/escalação para orientar o modelo
- `conversational-memory`: sessão multi-turno com memória de campos coletados e perguntas de clarificação incrementais
- `grounding-validation`: rubricas de qualidade, anti-alucinação, marcação fato/inferência/ausente e retry orientado

### Modified Capabilities

- (nenhuma main spec arquivada em `openspec/specs/`; a triagem one-shot de `filafacil-modelos-inference` permanece e é estendida pelo fluxo conversacional)

## Impact

- Novos pacotes sob `com.mundodev.filafacil.inference.prompt` (library, few-shot, session, grounding)
- `HttpOpenAiCompatibleGateway` / `MockInferenceGateway` / `OperationalTriageService` passam a consumir prompts da biblioteca
- `MenuController`: nova opção de atendimento conversacional
- Resources: `src/main/resources/inference/prompts/*.md|json` e `fewshot/*.json`
- Continua terminal + JSON local; sem fine-tuning, sem UI web, sem dados reais sensíveis
- Referência: PDF *Prompt Engineering & Interaction Básico + Intermediário – FilaFácil Logística*
