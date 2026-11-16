# Spec Delta

## Purpose

Priorizar uma ocorrência só depois de separar intenção, entidades e evidências, e bloquear a conclusão quando a base não existe.

## ADDED Requirements

### Requirement: Quatro etapas
O sistema MUST conduzir a priorização em quatro etapas separadas: identificar a intenção, extrair as entidades, validar as evidências e gerar a resposta.

#### Scenario: Etapas visiveis
- **WHEN** a priorização de uma ocorrência é executada
- **THEN** o resultado registra as quatro etapas nessa ordem

### Requirement: Conclusao sem base
O sistema MUST impedir uma prioridade apresentada como fato quando a etapa de evidências não encontra base na mensagem, na sessão ou numa fonte fornecida.

#### Scenario: Sem evidencia
- **WHEN** a ocorrência não traz evidência para a prioridade
- **THEN** a resposta final não publica a prioridade como fato
