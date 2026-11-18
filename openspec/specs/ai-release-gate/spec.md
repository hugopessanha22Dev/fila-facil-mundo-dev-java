# ai-release-gate Specification

## Purpose

Comparar a versão candidata com a atual e só promover quando os critérios bloqueantes passarem.

## Requirements

### Requirement: Bloqueio critico
O sistema MUST bloquear a promoção quando a candidata piorar groundedness, aderência ao formato, tool calling, segurança ou custo além do limite, e MUST listar os casos que falharam.

#### Scenario: Mais respostas sem evidencia
- **WHEN** a candidata tem mais respostas sem evidência do que a versão atual
- **THEN** a promoção é bloqueada e o caso falho entra no relatório

### Requirement: Alerta nao bloqueia
O sistema MUST registrar alerta quando só a relevância cair dentro da faixa permitida, e MUST permitir a promoção se os critérios bloqueantes continuarem dentro do limite.

#### Scenario: Relevancia leve
- **WHEN** a relevância cai e groundedness, formato, ferramenta, segurança e custo seguem no limite
- **THEN** a decisão é aprovada e o relatório marca alerta

### Requirement: Revisao humana
O sistema MUST encaminhar caso ambíguo para revisão humana e MUST registrar a versão avaliada, os casos falhos e a decisão tomada.

#### Scenario: Caso ambiguo
- **WHEN** um caso é marcado como ambíguo e ainda não há decisão humana
- **THEN** a promoção não ocorre e o relatório indica revisão humana
