# Proposal

## Why

A FilaFácil já tem ferramentas mock de triagem (status, ocorrência, prioridade e escala humana), mas elas são chamadas soltas: não há um agente que planeje, execute uma ferramenta por vez, observe o retorno e pare com guardrail. O case Mundo Dev **Agentic Systems Básico + Intermediário** pede esse operador automático para atraso, endereço incorreto, falha de coleta e ocorrência crítica, em até 6 horas, sem inventar dado.

## What Changes

- Definir objetivo, escopo logístico, ferramentas permitidas e critérios de parada (resolvido, dado essencial ausente, falha repetida, limite de passos ou risco)
- Expor ferramentas com schema: consultar coleta/entrega, buscar cliente, registrar ocorrência, consultar regras, notificar, calcular prioridade e escalar para humano
- Rodar o fluxo interpretar, planejar, executar, observar, atualizar estado, revisar e gravar trace, com uma ferramenta por passo
- Simular quatro integrações: API de coletas/entregas, banco de clientes e ocorrências, fila de notificações e a base de regras já existente
- Recusar ação sensível sem dado mínimo, bloquear pedido fora do domínio, interromper loop e escalar incerteza
- Avaliar os cenários do case (sucesso, código inexistente, ambíguo, ferramenta indisponível, erro ao registrar, crítico, fora de escopo, loop e ocorrência incompleta) com sucesso, custo, latência, passos, ferramentas, falhas, escalonamentos e aderência
- Manter o menu 0–12 e o contrato das tools de triagem; o agente é um comando novo
- Premissa: o plano de cada passo é uma política local determinística, sem chamada paga de modelo. O gráfico de pesos não extraiu números; o texto dá maior peso a planejamento e controle de estado

## Capabilities

### New Capabilities

- `agent-charter`: objetivo, escopo, limites de passo e critérios de parada
- `agent-tool-contracts`: schemas, validação e retorno estruturado das ferramentas operacionais
- `agent-step-loop`: um passo por ferramenta, estado e trace, sem dado inventado
- `simulated-ops-integrations`: API, banco, fila e base de regras com erro simulável
- `agent-guardrails`: permissão, confirmação de ação sensível, bloqueio fora do domínio e anti-loop
- `agent-scenario-eval`: cenários de sucesso, falha e guardrail, com métricas por execução

### Modified Capabilities

- (nenhuma; as specs de RAG e de triagem permanecem)

## Impact

- Novo pacote e CLI (ex.: `com.mundodev.filafacil.agent`), sem opção nova no menu
- Cenários em `src/main/resources/agent/`, fora de `inference/golden`
- Reuso das tools mock de triagem e da busca RAG de regras, embrulhadas nos schemas do agente
- README com o comando e o significado de cada parada
- Referência: PDF *Agentic Systems Básico + Intermediário – FilaFácil Logística* (Escola Mundo Dev). Este PDF não tinha change anterior neste repositório
