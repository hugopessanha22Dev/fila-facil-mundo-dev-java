# Spec Delta

## Purpose

Impedir que a esteira aceite entrada vazia ou resposta livre, limitar retries, acionar fallback de modelo e marcar intervencao humana quando a saida continua invalida.

## ADDED Requirements

### Requirement: Entrada recusada antes do modelo
O sistema MUST recusar um caso sem texto de solicitacao e MUST NOT chamar o modelo nesse caso.

#### Scenario: Caso sem texto
- **WHEN** um item do dataset chega sem mensagem
- **THEN** o caso e contado como falha de entrada e o modelo nao e chamado

### Requirement: Saida livre e rejeitada
O sistema MUST recusar resposta que nao seja um objeto JSON aderente ao schema da triagem.

#### Scenario: Texto livre
- **WHEN** o modelo devolve texto que nao e JSON do schema
- **THEN** a saida e rejeitada e o caso nao conta como sucesso de schema

### Requirement: Retry com teto e fallback
O sistema MUST repetir a chamada no maximo o numero configurado para o ambiente e, esgotado o teto, MUST tentar o modelo de fallback daquele ambiente uma vez.

#### Scenario: Fallback do ambiente
- **WHEN** as tentativas do modelo primario se esgotam com saida invalida
- **THEN** o sistema chama o modelo de fallback configurado para o ambiente e nao faz chamadas alem dessa tentativa extra

#### Scenario: Intervencao humana
- **WHEN** o fallback tambem devolve saida invalida
- **THEN** o caso e marcado para intervencao humana e nao e contado como classificacao aceita

### Requirement: Ambientes com guardrails proprios
O sistema MUST aplicar, em desenvolvimento, homologacao e producao simulada, secrets, quotas, rate limit, modelo primario e fallback proprios, sem reutilizar a chave de outro ambiente.

#### Scenario: Producao mais restrita
- **WHEN** a mesma esteira roda em producao simulada e em desenvolvimento
- **THEN** o teto de retries e o rate limit de producao sao menores ou iguais aos de desenvolvimento, e a chave de producao nao e a de desenvolvimento
