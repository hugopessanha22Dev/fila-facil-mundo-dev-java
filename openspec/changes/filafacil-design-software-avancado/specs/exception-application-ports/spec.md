# Spec Delta

## Purpose

Coordenar abertura, análise, escala, resolução, cancelamento e consulta da exceção por casos de uso que dependem de portas, não de adaptadores concretos.

## ADDED Requirements

### Requirement: Casos de uso do ciclo
O sistema MUST oferecer casos de uso para abrir, analisar, escalar, resolver, cancelar e consultar uma exceção, e MUST recusar a segunda exceção aberta do mesmo código de entrega e do mesmo tipo.

#### Scenario: Consulta depois da abertura
- **WHEN** um caso de uso abre uma exceção e outro consulta o identificador
- **THEN** a consulta devolve o mesmo código de entrega, tipo e status aberta

#### Scenario: Duplicidade
- **WHEN** já existe exceção aberta para a mesma entrega e o mesmo tipo
- **THEN** a segunda abertura é recusada e a primeira permanece aberta

### Requirement: Janela de resolucao
O sistema MUST recusar a resolução quando o instante da operação passa da janela informada na abertura, e MUST pedir escala nesse caso.

#### Scenario: Janela vencida
- **WHEN** a resolução ocorre depois do fim da janela
- **THEN** o status não vira resolvida e o caso de uso informa que a janela venceu

### Requirement: Portas sem adaptador concreto
O caso de uso MUST depender de contratos de repositório e de publicação, e MUST NOT referenciar repositório em memória, arquivo ou banco.

#### Scenario: Troca de adaptador
- **WHEN** o mesmo caso de uso recebe outro objeto que cumpre o contrato de repositório
- **THEN** abrir e consultar continuam produzindo o mesmo status
