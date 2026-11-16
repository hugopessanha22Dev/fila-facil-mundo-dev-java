# Spec Delta

## Purpose

Medir o gateway em entradas problemáticas e impedir que uma versão pior de configuração seja promovida.

## ADDED Requirements

### Requirement: Casos adversariais
O sistema MUST avaliar entrada ambígua, incompleta, longa e fora de escopo, em arquivos fora de `inference/golden`.

#### Scenario: Fora de escopo
- **WHEN** a avaliação roda o caso fora de escopo
- **THEN** o relatório marca o caso e não altera a contagem dos três golden antigos

### Requirement: Regressao bloqueia promocao
O sistema MUST comparar a versão candidata com a linha de base e MUST recusar a promoção quando acurácia ou aderência ao formato cair além do limite.

#### Scenario: Queda acima do limite
- **WHEN** a candidata perde acurácia acima do limite
- **THEN** a versão ativa permanece a anterior e o relatório diz que a promoção foi bloqueada

#### Scenario: Candidata dentro do limite
- **WHEN** a candidata mantém acurácia e formato dentro do limite
- **THEN** a promoção é aceita
