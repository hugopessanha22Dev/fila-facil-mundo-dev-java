# Spec Delta

## ADDED Requirements

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
