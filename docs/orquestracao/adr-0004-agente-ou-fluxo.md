# ADR 0004: agente versus fluxo deterministico

## Contexto

Atraso, ocorrencia e aprovacao precisam de ferramentas e estado. Classificacao e busca repetem o mesmo caminho.

## Decisao

Atraso, registro, plano inseguro e impacto alto vao ao agente. Triagem, classificador e busca seguem fluxo deterministico.

## Alternativa rejeitada

agente para classificacao e para a busca de politica.

## Consequencia

O classificador nao abre o loop do agente. O agente nao classifica prioridade isolada.

## Risco

Um pedido com as duas intencoes segue a primeira palavra da ordem fixa.

## Criterio de revisao

Rever se um pedido operacional passar a exigir ferramenta e classificacao no mesmo passo.
