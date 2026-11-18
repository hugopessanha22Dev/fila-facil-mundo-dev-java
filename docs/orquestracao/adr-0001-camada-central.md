# ADR 0001: camada central de orquestracao

## Contexto

Assistente, triagem, busca, agente, classificador e resumo ja existem, cada um com a propria entrada.

## Decisao

Uma entrada so classifica o pedido e delega ao fluxo correspondente.

## Alternativa rejeitada

chamada direta de cada aplicacao ao modelo, sem roteamento nem registro comum.

## Consequencia

O pedido passa por etapas, contrato e registro antes de voltar ao operador.

## Risco

Uma palavra ambigua pode escolher o fluxo errado. A ordem de classificacao e coberta por teste.

## Criterio de revisao

Rever se surgir um fluxo que a classificacao por palavras nao separa dos seis atuais.
