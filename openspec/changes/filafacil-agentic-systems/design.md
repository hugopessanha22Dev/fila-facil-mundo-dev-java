# Design

## Context

O case Agentic Systems pede um operador que, diante de atraso, endereço incorreto, falha de coleta ou ocorrência crítica, escolha uma ferramenta por vez, observe o retorno e pare com rastreio. O módulo de inferência já tem `OperationalTools` mock (status, coleta por id, ocorrência, prioridade, escala humana), usadas em teste unitário, sem laço de agente. O RAG já responde regra com citação. O menu permanece nas opções 0 a 12. O gráfico de pesos do PDF não extraiu números; o texto dá maior peso a planejamento e controle de estado.

## Goals / Non-Goals

**Goals:**

- Agente local com objetivo, escopo, teto de passos, teto de tentativas e critérios de parada
- Sete ferramentas com schema, erro estruturado e uma chamada por passo
- Quatro integrações simuladas: API de coleta/entrega, store de cliente/ocorrência, fila de notificação e consulta à base de regras
- Trace com interpretação, ferramenta, argumentos, resultado e decisão
- Avaliação dos nove cenários do case, sem chamada paga

**Non-Goals:**

- Chamar Groq, Gemini ou OpenAI no laço do agente
- Nova opção de menu
- Alterar o contrato de `OperationalTools` nem os requisitos de RAG
- Provisionar AWS ou fila real
- Inventar os números do gráfico de pesos

## Decisions

1. Pacote `com.mundodev.filafacil.agent`. A política de próximo passo é determinística: classifica a mensagem (atraso, endereço, coleta, crítico, fora de domínio) e escolhe a ferramenta a partir do estado já observado. Isso cobre planejamento e controle de estado sem custo de modelo.
2. Ferramentas do agente: `consultar_coleta`, `buscar_cliente`, `registrar_ocorrencia`, `consultar_regras`, `notificar`, `calcular_prioridade`, `escalar_humano`. Consulta, ocorrência, prioridade e escala delegam ao mock já existente quando o schema bater. `consultar_regras` chama a busca RAG do domínio correspondente e devolve trecho mais citação, ou ausência. Cliente e notificação são stores novos em memória, sem persistir em `data/coletas.json`.
3. Uma ferramenta por passo. Estado guarda código, cliente, status, regra citada, prioridade, ocorrência, notificações e motivo de escala. Saída final só copia esses campos; o que não veio da mensagem nem de ferramenta fica `ausente`.
4. Limites em `src/main/resources/agent/limits.json`: máximo de passos, tentativas por ferramenta e chamadas por execução. Repetir a mesma ferramenta com os mesmos argumentos encerra por loop. Falha repetida da mesma ferramenta encerra por falha. Código inexistente, dado essencial ausente e pedido fora do domínio encerram sem ação sensível.
5. Ação sensível (`registrar_ocorrencia`, `notificar`) só roda depois de o schema mínimo passar. O trace marca `confirmado` nesse passo. Não há tela de confirmação humana: a confirmação é a validação explícita antes da escrita. Escala humana é obrigatória para mensagem ambígua ou risco, e grava motivo, estado e dados coletados.
6. Erros simuláveis por cenário: API de consulta indisponível e falha de escrita da ocorrência. A fila guarda payload, instante e resultado. Os cenários ficam em `src/main/resources/agent/scenarios.json`, fora de `inference/golden`, para não mudar o total 3 do eval offline.
7. CLI `exec:java@agent` com `run --text ...` e `eval`. `eval` sai 0 só quando todo cenário tem a parada esperada e a aderência de guardrail. README documenta o comando, as paradas e que o plano é local. Custo estimado é contagem de passos, não preço de API.

## Risks / Trade-offs

- [Política determinística não cobre frase inesperada] → o caso cai em ambíguo e escala para humano, em vez de chutar ferramenta.
- [Reuso do mock de triagem tem schema mais curto] → o adaptador do agente exige os campos do case antes de delegar, e o teste prova a recusa.
- [Consulta RAG depende do índice local] → o teste de regra publica o corpus no diretório temporário, como o RAG já faz.
- [Confirmação sem operador humano] → o trace deixa explícito que a barreira é o schema, e o README diz isso.

## Migration Plan

- Adicionar o pacote, os JSON de limites e cenários, a execução `agent` no `pom.xml` e a seção no README.
- Rollback: remover o pacote, os recursos, a execução e a seção. `data/coletas.json` e o índice RAG publicado não são alterados pelo agente.

## Open Questions

- Nenhuma que mude o escopo. Frase sem código e sem cliente para por dado ausente. Frase com risco ou ambiguidade escala. Os números do gráfico ficam de fora.
