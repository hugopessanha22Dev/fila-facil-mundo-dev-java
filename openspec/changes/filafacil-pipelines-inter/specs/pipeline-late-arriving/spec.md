# Spec Delta

## Purpose

Trata dados atrasados com regra explicita e permite reprocessamento controlado e rastreavel por particao, periodo ou fonte.

## ADDED Requirements

### Requirement: Regra de late arriving data
O sistema SHALL aplicar uma regra documentada para dados atrasados: aceitar, rejeitar ou marcar, e contabilizar registros atrasados na execucao.

#### Scenario: Registro atrasado marcado
- **WHEN** um registro chega com timestamp operacional anterior a janela/checkpoint esperado e a regra e marcar
- **THEN** o registro e marcado como late-arriving, contabilizado e processado conforme a regra sem perda silenciosa

#### Scenario: Registro atrasado rejeitado
- **WHEN** a regra configurada e rejeitar dados atrasados
- **THEN** o registro nao entra na camada final e entra na contagem de descartados/atrasados

### Requirement: Reprocessamento controlado
O sistema SHALL permitir reprocessar particao, periodo ou fonte especifica de forma idempotente e rastreavel.

#### Scenario: Reprocessar uma fonte e periodo
- **WHEN** o operador solicita reprocessamento da fonte entregas para um periodo
- **THEN** apenas essa fonte/periodo e reprocessado, o resultado permanece idempotente e a execucao registra o escopo no log/auditoria
