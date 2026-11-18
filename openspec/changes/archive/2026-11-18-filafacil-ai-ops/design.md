# Design

## Context

See proposal.md — Why. O repositório já mede RAG, agente e o golden de três casos, e já promove prompt em `com.mundodev.filafacil.aidevops`. O registro da orquestração mascara segredo numa execução. Nenhum desses caminhos compara uma candidata com a versão atual para bloquear release, nem abre incidente com bloqueio temporário de um fluxo. O menu permanece nas opções 0 a 12. O projeto compila em Java 17 e os testes usam mock.

## Goals / Non-Goals

**Goals:**

- Ficha dos quatro fluxos e um gate local de promoção
- Recusa de injeção e de vazamento, com pergunta logística ainda aceita
- Incidente que bloqueia só o fluxo citado

**Non-Goals:**

- Substituir o gate de `aidevops`, o golden de três casos ou os cenários do agente
- Opção nova de menu, chamada paga, AWS ou alteração de `data/coletas.json`
- Usar os percentuais do gráfico como peso de configuração

## Decisions

1. O pacote novo é `com.mundodev.filafacil.aiops`. A CLI é `AiOpsMain`, no exec `ai-ops`. O menu não muda. Alternativa de uma opção 13: rejeitada porque o contrato do menu é 0 a 12.

2. `src/main/resources/aiops/fluxos.json` lista atendimento, rag, agente e classificador. Cada ficha traz dono, risco, modelo, prompt, entrada, saída, ferramentas, fonte, métricas e consequência. Groundedness, formato, tool calling, segurança e custo bloqueiam. Relevância só alerta. Os limites são valores pequenos de teste, não pesos do gráfico. Alternativa de copiar o peso do gráfico: rejeitada porque o número não saiu legível.

3. O golden fica em `src/main/resources/aiops/golden.json`, fora de `src/test/resources/inference/golden/`. Cada caso tem fluxo, entrada, evidência esperada e o resultado da versão atual e da candidata. A candidata que aumenta resposta sem evidência, quebra formato, falha ferramenta, sobe risco ou estoura custo bloqueia. A que só perde relevância aprova com alerta. Caso ambíguo fica em revisão até receber a decisão humana. O gate não chama `ReleaseStore`. Alternativa de reusar o golden de três casos: rejeitada porque aquele teste exige continuar com três arquivos.

4. A injeção é a frase que manda ignorar as regras ou mostrar o prompt. A pergunta de regra operacional sem essa frase passa. Vazamento é pedido de documento de outro cliente, segredo `gsk_` ou `sk-`, ou sequência de 11 dígitos ou mais. O trace grava o texto já mascarado. Ferramenta fora da lista da ficha, ou sem o argumento obrigatório, não executa. Alternativa de colocar a defesa só no texto do prompt: rejeitada porque o case exige controle fora do prompt.

5. Alerta de custo, latência, drift ou validação repetida guarda a causa informada: modelo, prompt, retrieval ou ferramenta. O incidente vive na raiz recebida pelo comando, não em `data/ai-delivery`. Rollback é decisão registrada no incidente. Enquanto o fluxo estiver bloqueado, execução nova desse fluxo é recusada e os demais seguem. Alternativa de chamar o rollback do `aidevops`: rejeitada porque aquele ponteiro pertence a outro gate.

## Risks / Trade-offs

- [Uma frase curta demais marca pergunta legítima como injeção] → só as frases explícitas de substituição e de exibição do prompt recusam.
- [O gate novo e o de `aidevops` podem divergir] → este gate não escreve o ponteiro daquele e o teste confere que a pasta dele não muda.
- [Bloquear o classificador pode parecer bloqueio geral] → o teste executa outro fluxo no mesmo estado e espera aceite.

## Migration Plan

A mudança só acrescenta pacote, recursos, CLI e uma seção no README. Remover esses arquivos desfaz o controle. RAG, agente, catálogo e promoção existente permanecem.

## Open Questions

Nenhuma. Os limites numéricos ficam em `fluxos.json`, escolhidos para o teste conseguir bloquear e alertar.
