# Spec Delta

## ADDED Requirements

### Requirement: Tempo maximo
O sistema MUST encerrar o fluxo quando o tempo de execução passar do teto configurado, mesmo que ainda haja passos disponíveis.

#### Scenario: Tempo esgotado
- **WHEN** o relógio do fluxo passa do teto antes de resolver o caso
- **THEN** o agente para por tempo e não faz nova chamada

### Requirement: Permissao por ferramenta
O sistema MUST conferir a permissão da ferramenta antes de cada chamada e MUST NOT executar ferramenta sem permissão.

#### Scenario: Chamada autorizada
- **WHEN** a ferramenta está na lista e a permissão do papel cobre a ação
- **THEN** a chamada segue e o trace registra a permissão conferida

#### Scenario: Chamada sem permissao
- **WHEN** a ferramenta existe, mas o papel não pode usá-la
- **THEN** a chamada não ocorre
