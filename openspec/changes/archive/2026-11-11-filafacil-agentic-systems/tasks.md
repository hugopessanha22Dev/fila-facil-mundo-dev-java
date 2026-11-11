# Tasks

## 1. Carta e ferramentas

- [x] 1.1 Carta, teto de passos e parada: teste de pedido logístico aceito, pedido fora do domínio sem ferramenta, e parada por dado essencial ausente ou por teto de passos
- [x] 1.2 Schemas das sete ferramentas: teste de consulta com código existente e inexistente, ocorrência sem descrição recusada, notificação sem código recusada, e escala com motivo, estado e dados coletados
- [x] 1.3 README com objetivo, ferramentas permitidas e o que cada parada significa

## 2. Integrações simuladas

- [x] 2.1 API de coleta/entrega e store de cliente/ocorrência: teste de consulta indisponível sem tratar como encontrado, e de erro de escrita sem gravar ocorrência
- [x] 2.2 Fila de notificação e consulta de regra: teste de item com payload, instante e resultado, e de regra encontrada com citação no trace
- [x] 2.3 Adaptadores sobre o mock de triagem e sobre a busca RAG, sem alterar o contrato antigo nem gravar em `data/coletas.json`

## 3. Laço e guardrails

- [x] 3.1 Laço de um passo: teste em que a consulta acontece antes do registro, o trace lista ferramenta e resultado em ordem, e cliente ausente não é inventado
- [x] 3.2 Guardrails: teste de registro incompleto sem gravação, registro completo com `confirmado` antes da escrita, ferramenta proibida sem chamada, falha repetida até o teto, e loop do mesmo argumento interrompido
- [x] 3.3 Política determinística cobrindo atraso, endereço, falha de coleta e caso crítico, com saída de status, ações, dados e próxima etapa

## 4. Avaliação

- [x] 4.1 Cenários em `src/main/resources/agent/scenarios.json`, fora de `inference/golden`: sucesso, código inexistente, ambíguo, consulta indisponível, erro ao registrar, crítico com humano, fora de escopo, loop e ocorrência sem dado mínimo
- [x] 4.2 Relatório com sucesso, custo estimado, latência, passos, ferramentas, falhas, escalonamentos e aderência, e `eval` saindo 0 só quando todos batem a parada esperada
- [x] 4.3 CLI `exec:java@agent` (`run`, `eval`) e execução no `pom.xml`, sem opção nova no menu
- [x] 4.4 `mvn test` verde, com o total do eval offline de inferência ainda em 3

## Workflow follow-up

- Smoke com texto real de atraso fica fora do `mvn test`
- Arquivar o change depois da revisão
