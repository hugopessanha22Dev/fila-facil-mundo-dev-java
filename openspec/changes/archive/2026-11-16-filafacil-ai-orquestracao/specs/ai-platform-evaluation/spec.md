# Spec Delta

## Purpose

Medir a plataforma num dataset simulado e deixar escritas as quatro decisões de arquitetura que justificam a camada central.

## ADDED Requirements

### Requirement: Dataset dos fluxos
O sistema MUST avaliar atendimento, triagem, busca, agente, classificador, resumo assíncrono, falha de vetor e falha de modelo em casos simulados.

#### Scenario: Fluxo esperado
- **WHEN** a avaliação percorre o dataset
- **THEN** cada caso compara o fluxo obtido com o fluxo esperado

#### Scenario: Ausencia sem invencao
- **WHEN** o caso espera ausência de evidência
- **THEN** a métrica de resposta inventada permanece em zero

### Requirement: Falha isolada
O sistema MUST manter a classificação utilizável quando apenas o banco vetorial falha.

#### Scenario: Classificador segue
- **WHEN** o vetor está indisponível e o caso é classificação
- **THEN** a classificação conclui e a falha fica restrita à busca

### Requirement: Quatro decisoes
O sistema MUST registrar as decisões de camada central, síncrono versus assíncrono, banco vetorial e agente versus fluxo determinístico, cada uma com a alternativa rejeitada e o critério de revisão.

#### Scenario: Decisoes legiveis
- **WHEN** as decisões são consultadas
- **THEN** as quatro estão presentes e cada uma nomeia a alternativa rejeitada
