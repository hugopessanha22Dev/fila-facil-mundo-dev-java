# Spec Delta

## Purpose

Dar a cada ferramenta operacional um schema de entrada, um retorno estruturado e um erro explícito quando o argumento não serve.

## ADDED Requirements

### Requirement: Consulta de coleta ou entrega
O sistema MUST buscar coleta ou entrega por código obrigatório e devolver status, cliente e indicador de caso encontrado.

#### Scenario: Codigo existente
- **WHEN** o agente consulta um código presente na base simulada
- **THEN** o retorno traz status e cliente estruturados

#### Scenario: Codigo ausente
- **WHEN** o agente consulta um código que não existe
- **THEN** o retorno informa não encontrado e não inventa status

### Requirement: Cliente e ocorrencia
O sistema MUST localizar cliente por identificador e MUST registrar ocorrência só quando código, tipo e descrição estiverem preenchidos.

#### Scenario: Ocorrencia sem descricao
- **WHEN** o registro chega sem descrição
- **THEN** a ferramenta recusa com erro de validação e nada é gravado

### Requirement: Regras, prioridade, notificacao e escala
O sistema MUST consultar regra operacional com citação, calcular prioridade, enviar notificação simulada e escalar para humano com motivo, estado do caso e dados já coletados.

#### Scenario: Escala com contexto
- **WHEN** o agente encaminha para humano
- **THEN** o registro contém motivo, estado atual e os dados coletados até ali

#### Scenario: Notificacao sem destino
- **WHEN** a notificação chega sem código de caso
- **THEN** a ferramenta recusa e a fila não recebe item
