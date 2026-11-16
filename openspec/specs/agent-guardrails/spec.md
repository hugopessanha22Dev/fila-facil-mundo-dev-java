# agent-guardrails Specification

## Purpose

Impedir registro, notificação e mudança de estado sem dado válido, e cortar loop, ferramenta fora de escopo e ferramenta indisponível.

## Requirements

### Requirement: Acao sensivel so com dado valido
O sistema MUST recusar registrar ocorrência, notificar ou alterar estado quando faltar dado mínimo, e MUST registrar no trace que a ação foi confirmada só depois da validação.

#### Scenario: Registro incompleto
- **WHEN** o agente tenta registrar ocorrência sem tipo ou sem descrição
- **THEN** a ação não ocorre e o trace marca argumento inválido

#### Scenario: Registro completo
- **WHEN** código, tipo e descrição estão presentes
- **THEN** o trace marca a confirmação e só então a ocorrência é gravada

### Requirement: Ferramenta fora de escopo ou indisponivel
O sistema MUST bloquear ferramenta que não está na lista permitida e MUST tratar ferramenta indisponível como falha, sem repetir além do teto daquela ferramenta.

#### Scenario: Ferramenta proibida
- **WHEN** o passo pede uma ferramenta fora da lista operacional
- **THEN** a chamada não acontece e o caso encerra como fora de escopo

#### Scenario: Falha repetida
- **WHEN** a mesma ferramenta falha até o teto de tentativas
- **THEN** o agente para de chamá-la e segue o critério de parada

### Requirement: Loop interrompido
O sistema MUST detectar repetição do mesmo passo com o mesmo argumento e MUST encerrar antes de ultrapassar o limite de chamadas.

#### Scenario: Mesmo passo de novo
- **WHEN** o agente escolheria de novo a mesma ferramenta com os mesmos argumentos
- **THEN** o fluxo para por loop e o trace registra o critério de parada

### Requirement: Plano inseguro
O sistema MUST barrar um plano inseguro antes de executar qualquer ferramenta.

#### Scenario: Plano barrado
- **WHEN** a mensagem pede um plano inseguro
- **THEN** nenhuma ferramenta é executada e o fluxo encerra como plano bloqueado

### Requirement: Aprovacao de alto impacto
O sistema MUST segurar notificação ou mudança de status de alto impacto até receber aprovação humana, e MUST executá-la quando a aprovação vier na mensagem.

#### Scenario: Sem aprovacao
- **WHEN** a mensagem pede notificação de alto impacto sem aprovação
- **THEN** a notificação não é enviada e o fluxo fica aguardando aprovação

#### Scenario: Aprovacao concedida
- **WHEN** a mesma notificação de alto impacto traz aprovação concedida
- **THEN** a notificação é enviada
