# Design

## Context

See proposal.md — Why. O repositório já tem assistente em `com.mundodev.filafacil.rag`, agente em `com.mundodev.filafacil.agent`, catálogo e `GatewayResiliente` em `com.mundodev.filafacil.inference.catalog`, e o pipeline de dados em `com.mundodev.filafacil.pipeline`. Cada um tem CLI própria. O menu permanece nas opções 0 a 12 e não contém a substring `rag`. `OfflineEvalRunnerTest` exige três JSON em `src/test/resources/inference/golden/`. O projeto compila em Java 17. Os testes usam mock.

## Goals / Non-Goals

**Goals:**

- Uma entrada que classifica o pedido e delega ao fluxo já existente
- Fila local, drenada de propósito, para embedding, reprocessamento e lote
- Contratos e falhas isoladas sem segundo circuit breaker
- Registro por execução e dataset próprio

**Non-Goals:**

- Reescrever RAG, agente, catálogo, pipeline de dados ou o orquestrador de exceções
- Opção nova de menu, Groq nos testes, OpenAI pago, AWS ou rede externa
- Alterar `data/coletas.json`, `limites.json` do catálogo ou os cenários do agente

## Decisions

1. O pacote novo é `com.mundodev.filafacil.orquestracao`. A CLI é `OrquestracaoMain`, no exec `ai-orquestracao`. O menu não muda. Alternativa de uma opção 13: rejeitada porque o contrato do menu é 0 a 12.

2. A classificação é por palavras, nesta ordem: embedding, reprocessar e lote vão para a fila; resumir vai para o resumo assíncrono; previsão do tempo e pedido não logístico param em fora de escopo; atraso, registrar ocorrência, plano inseguro e impacto alto vão para o agente; triagem vai para o classificador de ocorrência; classificar prioridade vai para o classificador; pergunta de operador sobre política vai para a busca; mensagem de cliente sobre regra vai para o atendimento. Qualquer outro texto fica fora de escopo. Alternativa de uma chamada de modelo para classificar a intenção: rejeitada porque o teste precisa ser determinístico e o mock não pode decidir o roteamento.

3. Atendimento chama o assistente RAG já existente e, sem evidência, devolve a frase `Nao encontrado nos documentos.`. Busca usa o recuperador e exige citação. Triagem e classificador chamam a tarefa `classificacao` do catálogo, passando pelo `GatewayResiliente`. Agente chama `AgentLoop` num mundo e num índice temporários. A ferramenta externa de endereço é nova, em memória, e não chama ViaCEP. Alternativa de reimplementar esses fluxos: rejeitada porque os requisitos deles já estão cobertos.

4. As etapas síncronas sempre aparecem: intenção, contexto, modelo, ferramenta, validação e encerramento. Busca e atendimento registram a ferramenta como não aplicada. Se o agente termina `escalado` ou `aguardando_aprovacao`, o status da plataforma é `revisao_humana`. Se a etapa principal e a alternativa falham, o status é `falhou` e nenhum campo ausente é preenchido.

5. A fila vive no processo. `executar` devolve o identificador e o status `aceito`. O teste chama o worker para drenar. Status possíveis: `aceito`, `processando`, `concluido`, `falhou`. Uma falha temporária consome um contador e gera uma segunda tentativa. O timeout usa relógio injetável. O worker só escreve sob a raiz recebida, nunca em `data/coletas.json`. Alternativa de thread ou broker: rejeitada porque o teste não pode depender de espera real.

6. `src/main/resources/orquestracao/contratos.json` lista modelo, vetor, coletas, entregas, ocorrências, store, ferramenta externa e fila, cada um com entrada, saída, timeout, tentativas, acesso e limite de uso. O vetor indisponível é uma flag do pedido: a busca devolve ausência e o classificador continua. O modelo indisponível é um provedor de teste que falha uma vez e devolve a alternativa; o registro marca fallback. Não se cria outro circuit breaker nem se edita `limites.json` do catálogo. Alternativa de copiar o gateway: rejeitada porque o circuito já existe lá.

7. Cada execução gera um registro com intenção, dados, ferramentas, contexto, modelo, versão, tentativas, erros, custo, latência, validação, fallbacks, status e feedback. O feedback começa `ausente` e pode ser anexado depois, sem mudar o status. Segredo no formato `gsk_` ou `sk-`, e sequência de 11 dígitos ou mais, saem do texto gravado. Cache de resposta da plataforma não entra: cada execução tem o próprio registro. A classificação continua podendo usar o cache que o catálogo já tem.

8. Os casos ficam em `src/main/resources/orquestracao/casos.json`, fora do golden e fora de `agent/scenarios.json`. O relatório confere o fluxo esperado, exige resposta inventada igual a zero nos casos de ausência e exige que o classificador conclua com o vetor indisponível. Os quatro ADRs ficam em `docs/orquestracao/` e não alteram `docs/excecoes/adr-0001-orquestrador.md`. Cada ADR traz contexto, decisão, alternativa rejeitada, consequência, risco e critério de revisão.

## Risks / Trade-offs

- [A palavra ocorrência serve à triagem e ao agente] → "registrar ocorrência" vai ao agente; "triagem" vai ao classificador; os dois textos têm teste.
- [Delegar ao agente pode tocar arquivo de coleta] → o teste usa diretório temporário e compara os bytes de `data/coletas.json`.
- [Um segundo gateway duplicaria circuito e limite] → modelo só entra pelo gateway já existente; a flag de teste substitui o provedor, não a política.
- [Os percentuais do PDF não são números de configuração] → o teste afirma ausência sem invenção e fallback ativo, sem gravar peso.

## Migration Plan

A mudança só acrescenta pacote, recursos, documentos e um exec. Remover esses arquivos desfaz a camada. RAG, agente, catálogo e menu permanecem.

## Open Questions

Nenhuma. Os limiares de timeout, retry e limite de uso ficam no JSON de contratos, com valores pequenos o bastante para o teste recusar a chamada seguinte.
