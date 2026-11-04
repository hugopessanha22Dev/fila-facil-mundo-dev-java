# Spec Delta

## Purpose

Define o resumo operacional da fila de coletas no terminal, com totais agregados de coletas, pacotes e distribuicao por prioridade e status.

## ADDED Requirements

### Requirement: Totais gerais de coletas e pacotes
O sistema MUST gerar um resumo com a quantidade total de coletas e a quantidade total de pacotes em todas as coletas.

#### Scenario: Resumo com dados
- **WHEN** o usuario solicita o resumo e existem coletas
- **THEN** o terminal MUST exibir o total de coletas e a soma de pacotes

#### Scenario: Resumo sem dados
- **WHEN** o usuario solicita o resumo e nao ha coletas
- **THEN** o terminal MUST exibir totais zerados (ou equivalente claro) sem erro fatal

### Requirement: Contagem por prioridade e status
O sistema MUST incluir no resumo a quantidade de coletas por cada prioridade (`baixa`, `media`, `alta`) e por cada status (`pendente`, `em_rota`, `concluida`).

#### Scenario: Distribuicao completa
- **WHEN** o usuario solicita o resumo com coletas de diferentes prioridades e status
- **THEN** o terminal MUST mostrar contagens por prioridade e por status cobrindo todos os valores do dominio
