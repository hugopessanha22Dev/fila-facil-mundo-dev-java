# Tasks

## 1. Roteamento

- [x] 1.1 Classificar o pedido, na ordem do design, como atendimento, triagem, busca, agente, classificador, resumo ou fora de escopo, e cobrir com teste que "registrar ocorrência" vai ao agente e "triagem" vai ao classificador de ocorrência

## 2. Fluxo síncrono

- [x] 2.1 Registrar as etapas intenção, contexto, modelo, ferramenta, validação e encerramento, com a ferramenta da busca marcada como não aplicada, com revisão humana quando o agente escala, e com falha sem dado inventado quando a alternativa também falha; verificar esses três resultados em teste

## 3. Fila

- [x] 3.1 Aceitar embedding, reprocessamento e lote sem esperar o worker, repetir uma falha temporária, encerrar por timeout com relógio manual e gravar a trilha aceito, processando e concluído; verificar em teste que a raiz do worker não altera `data/coletas.json`

## 4. Contratos

- [x] 4.1 Publicar o contrato das oito integrações e a consulta externa de endereço em memória, recusar a chamada acima do limite, devolver ausência quando o vetor falha e marcar fallback quando o modelo principal falha; verificar em teste que o classificador ainda conclui com o vetor indisponível

## 5. Registro

- [x] 5.1 Gravar os campos da execução, ocultar segredo e número longo, recusar regra sem evidência e anexar feedback sem mudar o status; verificar cada um desses resultados em teste

## 6. Avaliação

- [x] 6.1 Avaliar o dataset simulado fora do golden e fora dos cenários do agente, exigir fluxo esperado e resposta inventada igual a zero na ausência, e escrever as quatro decisões em `docs/orquestracao/`, cada uma nomeando a alternativa rejeitada; verificar a leitura desses arquivos em teste

## 7. Compatibilidade

- [x] 7.1 Expor a CLI no exec `ai-orquestracao` e uma seção no README; verificar que o menu segue na opção 12, sem opção 13 e sem a substring `rag`, que `data/coletas.json` permanece, que o golden continua com três JSON e que `mvn test` termina verde

## Workflow follow-up

- Arquivar o change depois da revisão
