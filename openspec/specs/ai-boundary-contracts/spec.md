# ai-boundary-contracts Specification

## Purpose

Exigir contrato e limite em cada integração da plataforma e isolar a falha para que uma camada caída não invente a resposta de outra.

## Requirements

### Requirement: Contrato de cada integracao
O sistema MUST exigir, para modelo, banco vetorial, coletas, entregas, ocorrências, store transacional, ferramenta externa e fila, a entrada, a saída, o erro, o timeout, o retry, o acesso e o limite de uso.

#### Scenario: Consulta externa simulada
- **WHEN** a ferramenta externa recebe um código permitido
- **THEN** o retorno segue o contrato e não usa rede externa

#### Scenario: Limite de uso
- **WHEN** a integração passa do limite de uso
- **THEN** a chamada é recusada e o erro informa o limite

### Requirement: Vetor indisponivel
O sistema MUST indicar ausência de contexto quando o banco vetorial falha e MUST NOT inventar uma regra.

#### Scenario: Busca sem vetor
- **WHEN** a busca depende do vetor e ele está indisponível
- **THEN** a resposta informa ausência de contexto e nenhuma regra é inventada

### Requirement: Modelo indisponivel
O sistema MUST acionar uma alternativa compatível com a tarefa quando o modelo principal está indisponível.

#### Scenario: Classificador em fallback
- **WHEN** o classificador encontra o modelo principal indisponível
- **THEN** o resultado vem da alternativa compatível e o registro marca fallback
