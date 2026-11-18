# ADR 0002: fluxo sincrono versus assincrono

## Contexto

Atendimento e classificacao precisam de resposta imediata. Embedding, reprocessamento e lote demoram.

## Decisao

O caminho sincrono devolve a resposta. Embedding, reprocessamento, lote e resumo entram numa fila local drenada de proposito.

## Alternativa rejeitada

broker externo e thread para esperar o worker no mesmo pedido.

## Consequencia

A resposta sincrona nao espera o worker. O teste avanca um relogio manual para o timeout.

## Risco

Um trabalho aceito fica parado ate o worker ser chamado.

## Criterio de revisao

Rever se o volume passar do que a fila em memoria do processo consegue drenar.
