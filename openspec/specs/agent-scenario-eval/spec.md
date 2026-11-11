# agent-scenario-eval Specification

## Purpose

Medir o agente nos cenários de sucesso, falha e guardrail do case, com custo, latência, passos, ferramentas, falhas e escalonamentos.

## Requirements

### Requirement: Cenarios de fluxo
O sistema MUST avaliar solicitação completa com sucesso, código inexistente, mensagem ambígua, consulta indisponível e erro ao registrar ocorrência.

#### Scenario: Sucesso completo
- **WHEN** a mensagem traz código existente e pedido de status de atraso
- **THEN** o relatório marca sucesso, lista as ferramentas usadas e a próxima etapa recomendada

#### Scenario: Codigo inexistente
- **WHEN** o código não existe na base
- **THEN** o cenário termina sem sucesso operacional e sem status inventado

### Requirement: Cenarios de guardrail
O sistema MUST avaliar caso crítico com humano, ferramenta fora de escopo, loop interrompido e ocorrência sem dados mínimos.

#### Scenario: Fora de escopo
- **WHEN** o cenário pede ação não logística
- **THEN** o relatório marca bloqueio de guardrail e zero ferramentas executadas

#### Scenario: Sem dados minimos
- **WHEN** a ocorrência não tem descrição
- **THEN** o relatório marca falha de validação e nenhuma ocorrência gravada

### Requirement: Metricas por cenario
O sistema MUST calcular, por cenário, sucesso, custo estimado, latência, número de passos, ferramentas usadas, falhas, escalonamentos e aderência aos guardrails.

#### Scenario: Relatorio agregado
- **WHEN** a avaliação percorre todos os cenários
- **THEN** o relatório mostra as oito métricas e o total de cenários
