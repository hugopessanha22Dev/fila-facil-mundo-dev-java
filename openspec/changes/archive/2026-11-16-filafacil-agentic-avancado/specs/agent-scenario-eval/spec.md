# Spec Delta

## ADDED Requirements

### Requirement: Cenarios avancados
O sistema MUST avaliar erro temporário resolvido no retry, erro permanente com escala humana, plano inseguro bloqueado e ação sensível aguardando aprovação, além dos nove cenários já existentes.

#### Scenario: Retry temporario
- **WHEN** o cenário marca falha temporária na primeira tentativa
- **THEN** o relatório marca o caso como resolvido depois do retry

#### Scenario: Plano bloqueado
- **WHEN** o cenário pede plano inseguro
- **THEN** o relatório marca plano bloqueado e zero ferramentas executadas

### Requirement: Metricas de trajetoria
O sistema MUST acrescentar, por cenário, retries, robustez, segurança e qualidade da trajetória, sem deixar de mostrar as oito métricas já exigidas.

#### Scenario: Relatorio com trajetoria
- **WHEN** a avaliação percorre os cenários novos e os nove anteriores
- **THEN** cada cenário traz retries, robustez, segurança e qualidade da trajetória
