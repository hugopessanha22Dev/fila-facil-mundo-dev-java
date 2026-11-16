# Spec Delta

## Purpose

Preferir incerteza explícita a prazo, status, responsável, política ou código operacional que não esteja numa fonte autorizada.

## ADDED Requirements

### Requirement: Fonte autorizada
O sistema MUST aceitar como base apenas a mensagem do operador, o contexto já confirmado na sessão, evidências fornecidas e o resultado de ferramenta autorizada.

#### Scenario: Codigo sem fonte
- **WHEN** a resposta apresenta um código operacional que não está na mensagem, na sessão nem numa evidência fornecida
- **THEN** esse código não entra como fato

### Requirement: Resposta calibrada
O sistema MUST indicar incerteza ou pedir clarificação quando não houver base para prazo, status, responsável, política interna ou código operacional.

#### Scenario: Prazo sem evidencia
- **WHEN** a resposta afirma um prazo ausente nas fontes autorizadas
- **THEN** a saída aceita marca incerteza e não publica esse prazo como fato
