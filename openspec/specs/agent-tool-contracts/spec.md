# agent-tool-contracts Specification

## Purpose

Dar a cada ferramenta operacional um schema de entrada, um retorno estruturado e um erro explícito quando o argumento não serve.

## Requirements

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

### Requirement: Consulta de entrega
O sistema MUST oferecer consulta de entrega com código obrigatório e retorno estruturado, e MUST manter a consulta já usada pelos fluxos atuais.

#### Scenario: Entrega existente
- **WHEN** a consulta de entrega recebe um código presente na base simulada
- **THEN** o retorno traz status e cliente, sem inventar campo ausente

### Requirement: Fila, historico e portal
O sistema MUST publicar mensagem numa fila simulada, MUST consultar o histórico do caso e MUST consultar um portal interno simulado, todos com retorno estruturado e sem rede externa.

#### Scenario: Publicacao na fila
- **WHEN** a publicação recebe código e mensagem
- **THEN** a fila simulada guarda o item e o retorno informa a publicação

#### Scenario: Historico vazio
- **WHEN** o caso ainda não tem ocorrência
- **THEN** o histórico volta vazio e não inventa evento

### Requirement: Erro temporario e permanente
O sistema MUST repetir a ferramenta numa falha temporária até o teto de tentativas e MUST escalar para humano quando a falha permanente atingir esse teto.

#### Scenario: Falha temporaria
- **WHEN** a primeira tentativa falha por erro temporário e a seguinte responde
- **THEN** o fluxo usa a resposta da tentativa seguinte

#### Scenario: Falha permanente
- **WHEN** a ferramenta falha de forma permanente até o teto de tentativas
- **THEN** o agente escala para humano e não trata o caso como resolvido
