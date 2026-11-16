# Tasks

## 1. Tempo e permissão

- [x] 1.1 Tempo máximo: teste com relógio manual de que o fluxo para por tempo sem nova chamada, e de que os tetos continuam 8 passos, 2 tentativas e 8 chamadas
- [x] 1.2 Permissão: teste de que a chamada autorizada registra a conferência e de que a chamada sem permissão não executa

## 2. Ferramentas e erros

- [x] 2.1 Consulta de entrega, fila, histórico e portal: teste de retorno estruturado, de histórico vazio sem evento inventado, e de que o atraso `ENT-100` continua começando em `consultar_coleta`
- [x] 2.2 Erro temporário e permanente: teste de que o retry seguinte resolve, de que a falha permanente escala, de que a consulta indisponível continua em `ferramenta_indisponivel`, e de que `run(String)` não zera o flag

## 3. Trajetória

- [x] 3.1 Etapas e resumo: teste de que o atraso resolvido lista a resposta final entre as etapas e de que a aprovação pendente entra no resumo como pendência

## 4. Guardrails novos

- [x] 4.1 Plano inseguro: teste de que nenhuma ferramenta executa e a parada é `plano_bloqueado`
- [x] 4.2 Alto impacto: teste de que sem aprovação a notificação não sai e a parada é `aguardando_aprovacao`, de que com aprovação concedida a notificação sai, e de que o registro comum de ocorrência continua resolvido com um aviso

## 5. Avaliação

- [x] 5.1 Cenários e métricas: teste de treze cenários, com os nove anteriores ainda presentes, e de que o relatório traz retries, robustez, segurança e qualidade da trajetória

## 6. Compatibilidade

- [x] 6.1 README com tempo máximo, aprovação humana e plano barrado antes da chamada; menu segue na opção 12 e sem opção 13; o fluxo não altera `data/coletas.json`; `mvn test` verde

## Workflow follow-up

- Arquivar o change depois da revisão
