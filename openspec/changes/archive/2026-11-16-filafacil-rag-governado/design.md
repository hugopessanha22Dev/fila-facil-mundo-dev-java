# Design

## Context

O pacote `com.mundodev.filafacil.rag` já ingere sete JSON, parte o texto, deduplica, troca a versão de um documento sem mexer nos outros, filtra domínio e acesso, combina termo e vetor, reranqueia, cita documento, versão e seção, e guarda sessão com TTL. `RagTest` exige exatamente esses sete domínios e a frase `Nao encontrado nos documentos.`. O menu não pode ganhar a opção 13 nem o texto `rag`. Ver proposal.md para o motivo.

## Goals / Non-Goals

**Goals:**

- Oitavo documento e status de vigência sem perder as sete famílias
- Namespace histórico e de tenant na busca já existente
- Expansão, compressão e top-k dinâmico configuráveis
- Sessão com perfil, formato e recusa de segredo
- Relatório das quatro falhas novas com a ação correspondente

**Non-Goals:**

- Fine-tuning e chamada paga
- Reescrever o índice em Python ou num banco vetorial externo
- Trocar a frase de ausência
- Opção nova de menu
- Arquivo novo em `inference/golden`

## Decisions

1. O PDF recomenda Python. O repositório já tem o RAG em Java, então a governança entra nesse pacote. Um serviço novo em outra linguagem deixaria dois índices.
2. O histórico operacional é `src/main/resources/rag/corpus/historico-operacional.json`, domínio `historico`. `CorpusCatalog.DOMINIOS` e o teste que exige tamanho 7 passam a oito no mesmo passo. Os sete JSON atuais não mudam de texto.
3. O documento ganha status, hash, versão vizinha, tenant e namespace, com padrão vigente, tenant `operacao` e namespace `ativo` quando o JSON antigo não traz o campo. Assim a busca atual continua vendo o corpus vigente. Substituído grava namespace `historico`. Expirado e em revisão só aparecem se a busca pedir esse status; em revisão exige acesso `revisor`.
4. Cada ingestão grava um registro com id da execução e ids dos documentos reprocessados. Republicar um documento não altera o vetor dos outros, que o teste de atualização incremental já observa.
5. Sinônimos ficam em `rag/expansao.json`. A expansão soma termos e não apaga a pergunta. Se o sinônimo apontar outro domínio, a avaliação marca expansão inadequada.
6. A compressão descarta um chunk cujo texto inteiro já está num chunk melhor colocado. O chunk que contém o termo distintivo da pergunta permanece. O top-k simples continua o da configuração; pergunta com dois domínios pode ir até `maxTopK`, novo campo de `retrieval.json`, sem mudar o corte nem o top-k das perguntas de um domínio.
7. A sessão ganha tenant, perfil e formato. Arquivo antigo sem esses campos carrega com tenant `operacao`. A leitura com outro tenant devolve ausência. Sequência de 11 dígitos e prefixo `gsk_` ou `sk-` não são gravados.
8. As falhas novas ficam em `rag/falhas-governanca.json`, fora das perguntas que já precisam fechar a avaliação com cobertura e groundedness em 1.0. Cada falha sai com a ação: bloquear na busca ativa, ampliar o teto, preservar a intenção ou religar o reranking.

## Risks / Trade-offs

- [O teste das sete famílias quebra no meio da implementação] → o documento novo e a asserção mudam na mesma tarefa.
- [Top-k dinâmico altera perguntas que já passam] → ele só sobe quando há dois domínios; o teto novo fica acima do top-k atual.
- [Campo novo no documento quebra o construtor] → a tarefa atualiza os pontos que montam o documento e o teste de parse desconhecido continua rejeitando domínio fora da lista.
- [Sessão antiga sem tenant seria ilegível] → a leitura trata tenant ausente como `operacao`.

## Migration Plan

- Acrescentar o JSON, os campos opcionais, a expansão, as falhas e a seção no README.
- Rollback: remover o oitavo documento, os campos novos e a seção. As sete famílias, a frase de ausência e o menu permanecem.

## Open Questions

- Nenhuma que mude o escopo. Os números do gráfico de pesos não entram na configuração.
