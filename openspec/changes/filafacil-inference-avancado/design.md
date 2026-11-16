# Design

## Context

O pacote `com.mundodev.filafacil.inference` já roteia triagem entre `LIGHT` e `DETAILED`, valida o JSON da triagem, faz fallback e mede três casos em `src/test/resources/inference/golden/`. `OfflineEvalRunnerTest` exige que essa pasta continue com três JSON. O menu permanece nas opções 0 a 12. O PDF avançado acrescenta cinco tarefas, catálogo, versão de configuração, circuito, rate limit e regressão. O gráfico de pesos não extraiu números. Os testes ficam no mock, sem Groq e sem OpenAI pago.

## Goals / Non-Goals

**Goals:**

- Catálogo das cinco tarefas e de dois modelos, com a escolha e o fallback testáveis
- Configuração versionada por tarefa, com streaming só onde a validação do JSON completo não é obrigatória
- Gateway único com rate limit, backoff, timeout, circuit breaker e métricas
- Quatro otimizações com política explícita
- Casos adversariais e um portão de promoção

**Non-Goals:**

- Chamada paga dentro de `mvn test`
- Nova opção de menu
- Arquivo novo em `inference/golden`
- Speculative decoding
- Reescrever o schema `justificativaIntervencencao` da triagem
- Substituir a esteira `aidevops`, que continua dona do release de prompt de entrega

## Decisions

1. Novo subpacote `com.mundodev.filafacil.inference.catalog`. Os JSON ficam em `src/main/resources/inference/catalog/`: `tarefas.json`, `modelos.json`, `config-v1.json`, `config-v2.json`, `otimizacao.json` e `limites.json`. A versão ativa e a anterior ficam em memória no teste; a CLI grava o ponteiro em `data/inference/ativa.json`.
2. Tarefas: `classificacao`, `extracao`, `resumo`, `resposta_assistida`, `busca_contextual`. Modelos: `barato` (janela curta, sem tool use, privacidade baixa) e `robusto` (janela longa, tool use, privacidade alta). Classificação simples escolhe `barato`. Extração, tool use, risco alto e busca contextual escolhem `robusto`. Se o fallback candidato tiver privacidade abaixo da tarefa, a seleção devolve recusa em vez de trocar.
3. `config-v1` guarda temperature, top-p, max tokens, stop sequences e seed. Streaming é verdadeiro só em `resumo` e `resposta_assistida`. Extração tem temperature menor ou igual à da classificação. Promover copia o ponteiro; rollback restaura o anterior e mantém as duas versões no catálogo.
4. `GatewayResiliente` envolve o mock. Toda execução devolve a tarefa e o nome `gateway-central`. Rate limit e timeout vêm de `limites.json`. O retry usa um relógio injetável, com espera dobrada a cada tentativa; o teste usa espera zero. O circuito abre ao atingir o limiar, a chamada seguinte não entra no mock, uma chamada de prova em half-open pode fechar. JSON inválido na extração conta como falha e dispara a segunda tentativa antes de publicar o texto.
5. Cache só para `classificacao` com a mesma chave de texto. Busca contextual ignora o cache. Batching agrupa duas classificações compatíveis numa execução do mock. Compressão remove espaço repetido. Poda corta o miolo do contexto e preserva o marcador `evidencia-critica`. `otimizacao.json` traz uso, proibição e risco das quatro estratégias. Speculative decoding não entra.
6. Casos ambíguo, incompleto, longo e fora de escopo ficam em `src/main/resources/inference/adversarial/`. A regressão compara acurácia e formato com `baseline.json`. Acima do limite, a promoção não muda a versão ativa. O README explica as cinco tarefas, o circuito e por que a busca contextual não usa cache.

## Risks / Trade-offs

- [O router antigo da triagem continua por tamanho e palavra de urgência] → esta camada escolhe modelo para as cinco tarefas novas e não troca a heurística da opção de menu já entregue.
- [Backoff real atrasaria o teste] → a espera é uma porta; produção usa o dobro a cada tentativa e o teste usa zero.
- [Circuit breaker em memória zera com o processo] → o PDF aceita execução local e o README diz isso.
- [A esteira DevOps já bloqueia release de prompt] → o portão daqui só promove versão de configuração de inferência.

## Migration Plan

- Acrescentar o subpacote, os JSON de catálogo, os adversariais, a seção no README e, se a CLI precisar de entrada, a execução no `pom.xml` sem opção nova de menu.
- Rollback: remover o subpacote, os recursos novos e a seção. O golden de três casos e `data/coletas.json` não mudam.

## Open Questions

- Nenhuma que mude o escopo. Os limiares numéricos de circuito, rate limit e regressão ficam no JSON de limites, escolhidos para o teste conseguir abrir o circuito com poucas falhas.
