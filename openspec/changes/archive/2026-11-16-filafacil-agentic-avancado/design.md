# Design

## Context

O pacote `com.mundodev.filafacil.agent` já classifica a mensagem, chama uma ferramenta por passo e cobre nove cenários. A consulta dos fluxos atuais, inclusive `ENT-100`, usa `consultar_coleta`. `run(String)` não zera os flags de falha. O teste de avaliação exige exatamente nove cenários. Ver proposal.md para o motivo.

## Goals / Non-Goals

**Goals:**

- Tempo máximo e permissão conferida antes da chamada
- Ferramentas novas sem mudar a primeira consulta dos fluxos atuais
- Falha temporária que acerta no retry e falha permanente que escala
- Plano inseguro sem chamada e alto impacto só com aprovação
- Resumo com evidências, ações e pendências
- Avaliação em treze cenários, com as métricas novas

**Non-Goals:**

- Modelo pago, fila externa ou navegador real
- Trocar a parada dos nove cenários atuais
- Opção nova de menu
- Reescrever `data/coletas.json`

## Decisions

1. O relógio do fluxo é injetável. O padrão continua o relógio do sistema. `limits.json` ganha `maxDurationMs` alto o bastante para os testes atuais não esgotarem o tempo. O teste de tempo avança o relógio manual. Os tetos de 8 passos, 2 tentativas e 8 chamadas permanecem.
2. A permissão fica num mapa em `agent/permissoes.json`. A lista `PERMITIDAS` continua o conjunto que o fluxo pode oferecer. A conferência acontece antes de `call`. Ferramenta fora da lista segue como fora de escopo.
3. `consultar_entrega`, `publicar_fila`, `consultar_historico` e `consultar_portal` entram na lista. O planejador dos fluxos atuais continua chamando `consultar_coleta` primeiro. A fila nova grava no mesmo registro simulado de avisos, com resultado próprio, e o fluxo de ocorrência continua em `notificar`. Portal e histórico leem só a base em memória.
4. Falha temporária é um contador no mundo simulado: a primeira consulta falha e a seguinte responde. Ela não usa o flag de consulta indisponível, que continua falhando até o teto e parando em `ferramenta_indisponivel`. Falha permanente é outro flag e, no teto, para em `escalado`. O flag de erro de escrita continua parando em `falha_ferramenta`. `run(String)` não limpa nenhum desses flags.
5. A frase `plano inseguro` é classificada antes das outras intenções e encerra em `plano_bloqueado` com zero ferramentas executadas. `impacto alto` sem `aprovacao concedida` para antes de notificar e encerra em `aguardando_aprovacao`. Com `aprovacao concedida`, a notificação segue. O fluxo comum de ocorrência não contém essas frases, então continua resolvido e com um aviso.
6. A trajetória ganha a lista de etapas percorridas e o resumo. A linha atual do trace, com índice, ferramenta, resultado e decisão, não muda. Os nove cenários permanecem em `scenarios.json`. Os quatro novos usam campos opcionais, com padrão falso. O teste que exige tamanho 9 passa a exigir 13 no mesmo passo.

## Risks / Trade-offs

- [Classificar `impacto alto` como caso crítico mudaria a parada para `escalado`] → a frase é tratada no planejador, depois da consulta, e não entra na regra de caso crítico.
- [Trocar `consultar_coleta` por `consultar_entrega` no atraso quebra o teste do `ENT-100`] → a primeira consulta dos fluxos atuais permanece.
- [O teste dos nove cenários fica vermelho no meio da implementação] → os quatro cenários e a asserção mudam na mesma tarefa.
- [Tempo máximo dispara nos testes atuais] → o teto novo fica largo e o relógio manual só entra no teste de tempo.

## Migration Plan

- Acrescentar os JSON, os campos opcionais, as ferramentas e a seção no README.
- Rollback: remover as ferramentas novas, os quatro cenários e a seção. Os nove cenários, os tetos atuais e o menu permanecem.

## Open Questions

- Nenhuma que mude o escopo. Os percentuais do gráfico não entram na configuração.
