# Spec Delta

## ADDED Requirements

### Requirement: Etapas do fluxo
O sistema MUST registrar, na trajetória, as etapas de interpretação, planejamento, validação, execução, observação, atualização, reflexão, revisão, aprovação e resposta final que o caso percorreu.

#### Scenario: Caso resolvido
- **WHEN** um caso de atraso termina resolvido
- **THEN** a trajetória lista as etapas percorridas, inclusive resposta final

### Requirement: Resumo com pendencias
O sistema MUST devolver um resumo com evidências, ações executadas e pendências, e MUST marcar pendente a ação que ficou sem aprovação.

#### Scenario: Aprovacao pendente
- **WHEN** o fluxo para esperando aprovação
- **THEN** o resumo cita a ação pendente e não a apresenta como concluída
