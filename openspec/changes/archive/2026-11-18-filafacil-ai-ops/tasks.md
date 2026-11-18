# Tasks

## 1. Fichas

- [x] 1.1 Catalogar atendimento, RAG, agente e classificador com dono, risco, modelo, prompt, entrada, saída, ferramentas, fonte e métricas, e verificar em teste que groundedness abaixo do limite tem consequência de bloqueio

## 2. Gate de release

- [x] 2.1 Comparar versão atual e candidata no golden próprio, bloquear piora de groundedness, formato, ferramenta, segurança ou custo, aprovar com alerta a queda leve de relevância e segurar caso ambíguo até a decisão humana; verificar os três resultados em teste, sem gravar em `data/ai-delivery`

## 3. Segurança

- [x] 3.1 Recusar instrução que manda ignorar as regras ou mostrar o prompt, aceitar a pergunta logística, mascarar segredo e número longo e recusar ferramenta fora do escopo; verificar cada um desses resultados em teste

## 4. Incidente

- [x] 4.1 Registrar alerta de custo com a causa da camada, abrir incidente com severidade, dono, rollback e ação preventiva, e recusar só o fluxo bloqueado; verificar em teste que outro fluxo continua aceito

## 5. Compatibilidade

- [x] 5.1 Expor a CLI no exec `ai-ops` e uma seção no README; verificar que o menu segue na opção 12, sem opção 13 e sem a substring `rag`, que `data/coletas.json` permanece, que o golden antigo continua com três JSON e que `mvn test` termina verde

## Workflow follow-up

- Arquivar o change depois da revisão
