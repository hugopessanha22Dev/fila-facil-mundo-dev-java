# ai-run-record Specification

## Purpose

Guardar um registro único de cada execução de IA e impedir que a resposta saia quando um guardrail de entrada, saída, ferramenta ou dado sensível falha.

## Requirements

### Requirement: Campos da execucao
O sistema MUST registrar intenção, dados extraídos, ferramentas, contexto, modelo, versão da configuração, tentativas, erros, custo estimado, latência, validação, fallbacks e status final.

#### Scenario: Registro ao encerrar
- **WHEN** um fluxo síncrono termina
- **THEN** o registro contém esses campos e o feedback fica ausente até ser informado

### Requirement: Guardrail antes da resposta
O sistema MUST validar entrada, saída, ferramenta, escopo, evidência e dado sensível antes de devolver a resposta.

#### Scenario: Segredo na entrada
- **WHEN** a entrada contém um segredo ou um número longo de documento
- **THEN** a resposta não segue e o registro não guarda o segredo em claro

#### Scenario: Resposta sem evidencia
- **WHEN** a tarefa exige evidência e a validação não encontra fonte
- **THEN** a resposta não apresenta uma regra inventada

### Requirement: Feedback posterior
O sistema MUST aceitar feedback de uma execução já registrada sem reabrir o fluxo.

#### Scenario: Feedback anexado
- **WHEN** o operador informa feedback de uma execução existente
- **THEN** o registro passa a conter esse feedback e o status final permanece o mesmo
