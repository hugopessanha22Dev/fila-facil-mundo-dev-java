# Proposal

## Why

A FilaFácil já tem assistente com RAG, triagem, busca com citação, agente, classificador e resumo, mas cada um é chamado à parte. O case Mundo Dev **AI Architecture & Orchestration Básico + Intermediário + Avançado** pede a camada que falta: uma entrada só, que classifica a tarefa, escolhe o fluxo, isola a falha e registra a execução. O gráfico de pesos não extraiu números. As marcas de 100% e 0% do texto são regras de sucesso, não pesos de configuração.

## What Changes

- Receber o pedido operacional numa entrada só e classificá-lo como atendimento, triagem, busca, agente, classificador ou resumo
- Conduzir o fluxo síncrono em etapas: intenção, contexto, modelo, ferramenta, validação, fallback e encerramento ou revisão humana
- Mandar embedding, reprocessamento de documento e análise em lote para uma fila local com worker, retry, timeout e evento de status, sem segurar a resposta síncrona
- Exigir contrato de entrada, saída, erro, timeout, retry, acesso e limite de uso para modelo, banco vetorial, APIs internas, store transacional, ferramenta externa simulada e fila
- Se o vetor falhar, devolver ausência de contexto. Se o modelo principal falhar, usar alternativa compatível com a tarefa. Não inventar regra, status nem endereço
- Gravar, por execução, intenção, dados extraídos, ferramentas, contexto, modelo, versão de configuração, tentativas, erros, custo, latência, validação, fallback e status
- Barrar, antes da resposta, entrada inválida, saída sem evidência, ferramenta fora do escopo, dado sensível e caso que exige revisão humana
- Avaliar os fluxos num dataset simulado, fora dos três casos do eval antigo e fora dos cenários do agente
- Registrar quatro decisões: camada central em vez de chamada direta, síncrono versus assíncrono, banco vetorial em vez de fine-tuning ou prompt stuffing, e agente versus fluxo determinístico
- Manter o menu nas opções 0 a 12, `data/coletas.json` intacto e o mock como caminho dos testes

## Capabilities

### New Capabilities

- `ai-request-routing`: classifica o pedido e escolhe um dos seis fluxos já existentes, com entrada, saída, risco e validação declarados
- `ai-sync-flow`: conduz as etapas síncronas, aplica fallback e encerra ou pede revisão humana
- `async-ai-work`: fila e worker para embedding, reprocessamento e lote, com retry, timeout e evento de status
- `ai-boundary-contracts`: contratos e isolamento de falha do modelo, do vetor, das APIs internas, do store, da ferramenta externa e da fila
- `ai-run-record`: registro único da execução e bloqueio da resposta que fere guardrail
- `ai-platform-evaluation`: dataset simulado dos fluxos e as quatro decisões de arquitetura

### Modified Capabilities

- (nenhuma; agente, RAG, validação de saída e integrações simuladas continuam com os requisitos atuais e passam a ser chamados por esta camada)

## Impact

- Pacote novo `com.mundodev.filafacil.orquestracao`, CLI própria e seção no README
- Sem opção nova de menu e sem chamada de modelo pago nos testes
- Os três JSON de `src/test/resources/inference/golden/` e os cenários do agente permanecem
- ADRs em `docs/orquestracao/`, sem alterar o ADR de exceções já existente
