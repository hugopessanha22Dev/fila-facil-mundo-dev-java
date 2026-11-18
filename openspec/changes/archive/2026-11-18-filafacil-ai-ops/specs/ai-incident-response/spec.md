# Spec Delta

## Purpose

Abrir incidente de IA com severidade, dono e causa, e bloquear só o fluxo afetado até a mitigação.

## ADDED Requirements

### Requirement: Sinal e causa
O sistema MUST gerar alerta quando custo, latência, drift ou falha repetida de validação passar do limite, e MUST classificar a causa como modelo, prompt, retrieval ou ferramenta.

#### Scenario: Custo acima do limite
- **WHEN** o custo observado passa do limite do fluxo
- **THEN** um alerta é registrado e a causa nomeia a camada que falhou

### Requirement: Incidente com bloqueio
O sistema MUST abrir incidente com severidade, dono, mitigação, rollback, causa e ação preventiva, e MUST recusar execução nova do fluxo bloqueado.

#### Scenario: Fluxo bloqueado
- **WHEN** um incidente bloqueia o classificador
- **THEN** uma classificação nova desse fluxo é recusada e os outros fluxos continuam aceitos

#### Scenario: Rollback registrado
- **WHEN** a mitigação escolhida é rollback
- **THEN** o incidente registra o rollback e a ação preventiva
