# prompt-session-resume Specification

## Purpose

Retomar um atendimento de prompt com o estado que ainda importa, sem tratar a memória da conversa como documento operacional.

## Requirements

### Requirement: Retomada seletiva
O sistema MUST guardar a intenção, os campos pendentes e as evidências usadas, e MUST recarregar só esse estado quando o atendimento é retomado.

#### Scenario: Historico irrelevante
- **WHEN** uma sessão antiga é retomada e o histórico contém um trecho que não entra na decisão atual
- **THEN** o prompt remontado contém a intenção guardada e não contém esse trecho

### Requirement: Memoria nao vira fonte
O sistema MUST registrar as evidências usadas em cada resposta e MUST NOT aceitar um dado que exista só na memória da sessão como evidência documental.

#### Scenario: Regra so na memoria
- **WHEN** a sessão lembra um prazo que nenhuma evidência fornecida sustenta
- **THEN** esse prazo não é registrado como evidência da resposta
