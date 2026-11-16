# response-validation-pipeline Specification

## Purpose

Barrar saída estruturada inválida antes do operador, corrigindo só o formato e encaminhando o resto para revisão humana.

## Requirements

### Requirement: Cadeia de validacao
O sistema MUST validar a saída na ordem schema, parser, campos obrigatórios, tipos, consistência entre campos e rubrica de qualidade.

#### Scenario: Campo obrigatorio ausente
- **WHEN** o JSON parseia e omite um campo obrigatório
- **THEN** a validação falha nessa etapa e a saída não é aceita

### Requirement: Autocorrecao e bloqueio
O sistema MUST tentar corrigir apenas o formato, como cerca de código em volta de um JSON válido, e MUST bloquear para revisão humana quando o conteúdo continua inválido depois do retry.

#### Scenario: Cerca de codigo
- **WHEN** a resposta traz um JSON válido dentro de uma cerca de código
- **THEN** o formato é corrigido e o JSON interno pode seguir na cadeia

#### Scenario: Retry esgotado
- **WHEN** as tentativas terminam e a saída segue inválida
- **THEN** o resultado fica bloqueado para revisão humana e o texto inválido não é a saída aceita
