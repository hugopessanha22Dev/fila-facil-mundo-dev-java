# Proposal

## Why

A FilaFácil já avalia RAG, agente, catálogo e a entrega de prompt, e já guarda um registro por execução. O case Mundo Dev **AI Ops, Evaluation & Security Básico + Intermediário + Avançado** pede o que ainda não existe: um gate que bloqueia release ruim, defesa contra injeção e vazamento, e um incidente com dono, rollback e bloqueio temporário do fluxo. O gráfico de pesos não extraiu números. O texto só diz que groundedness e segurança pesam mais que as outras métricas.

## What Changes

- Catalogar atendimento, RAG de políticas, agente de ocorrência e classificador, cada um com dono, risco, modelo, prompt, entrada, saída, ferramentas, fonte e critério de bloqueio
- Comparar a versão candidata com a atual num golden sem dado real e bloquear a promoção quando groundedness, formato, tool calling, segurança ou custo piorar além do limite
- Tratar queda leve de relevância como alerta, sem impedir a promoção
- Registrar a versão avaliada, os casos que falharam e a decisão, inclusive quando um caso ambíguo for para revisão humana
- Recusar instrução que tenta substituir a regra interna ou extrair prompt, segredo ou documento de outro cliente, e deixar passar a pergunta logística legítima
- Mascarar segredo e número longo na entrada e na saída do trace
- Abrir incidente com severidade, dono, causa, mitigação, rollback e ação preventiva, e bloquear só o fluxo afetado enquanto o incidente estiver aberto
- Diferenciar falha de modelo, prompt, retrieval e ferramenta
- Manter o menu nas opções 0 a 12, os três JSON do eval antigo e o gate de promoção que já existe

## Capabilities

### New Capabilities

- `ai-monitored-flows`: ficha operacional dos quatro fluxos, com dono, risco, métrica e consequência
- `ai-release-gate`: comparação entre versão atual e candidata, com bloqueio, alerta e revisão humana
- `ai-prompt-security`: defesa de injeção e vazamento, com mascaramento e ferramenta só dentro do escopo
- `ai-incident-response`: sinal de produção, incidente, causa, rollback e bloqueio temporário de um fluxo

### Modified Capabilities

- (nenhuma; registro de execução, avaliação de retrieval, guardrails do agente e a promoção já existente continuam com os requisitos atuais)

## Impact

- Pacote novo `com.mundodev.filafacil.aiops`, CLI própria e seção no README
- Golden novo fora de `src/test/resources/inference/golden/`
- Sem opção nova de menu, sem chamada paga e sem alterar `data/coletas.json`
