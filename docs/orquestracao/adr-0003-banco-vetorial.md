# ADR 0003: banco vetorial para conhecimento

## Contexto

Pergunta operacional precisa de trecho e fonte. Sem evidencia, a resposta nao pode completar a regra.

## Decisao

A busca e o atendimento usam o indice vetorial ja existente. Vetor indisponivel devolve ausencia de contexto.

## Alternativa rejeitada

fine-tuning do modelo e prompt stuffing do corpus inteiro na chamada.

## Consequencia

A citacao continua vindo do indice. A falha do vetor nao derruba o classificador.

## Risco

Um corte alto demais devolve ausencia mesmo com documento presente.

## Criterio de revisao

Rever se a ausencia passar a esconder documento que o indice ja contem.
