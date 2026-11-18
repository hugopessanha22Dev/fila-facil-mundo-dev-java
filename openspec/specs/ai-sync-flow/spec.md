# ai-sync-flow Specification

## Purpose

Conduzir o pedido síncrono por intenção, contexto, modelo, ferramenta e validação, e só então encerrar ou pedir revisão humana.

## Requirements

### Requirement: Etapas em ordem
O sistema MUST percorrer intenção, contexto, modelo, ferramenta e validação nessa ordem, e MUST registrar a etapa de ferramenta mesmo quando ela não se aplica.

#### Scenario: Busca sem ferramenta
- **WHEN** o fluxo escolhido é busca
- **THEN** a etapa de ferramenta fica registrada como não aplicada e a validação ocorre depois do contexto

### Requirement: Encerramento ou revisao
O sistema MUST devolver a resposta só depois da validação aprovada e MUST marcar revisão humana quando o fluxo pedir supervisão.

#### Scenario: Supervisao humana
- **WHEN** o fluxo indica que o caso precisa de supervisão
- **THEN** o status final é revisão humana e a resposta não é apresentada como concluída

### Requirement: Fallback de etapa
O sistema MUST tentar a alternativa compatível quando a etapa principal falha e MUST encerrar em falha, sem completar dado ausente, quando a alternativa também falha.

#### Scenario: Alternativa esgotada
- **WHEN** a etapa principal e a alternativa falham
- **THEN** o fluxo encerra em falha e não publica dado inventado
