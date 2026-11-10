# Spec Delta

## Purpose

Cataloga exemplos few-shot (positivos, negativos, contraexemplos e escalacao humana) para orientar o modelo com precisao, sem dados sensiveis reais de clientes.

## ADDED Requirements

### Requirement: Cobertura de tipos de exemplo
O catalogo MUST incluir exemplos positivos, negativos (ambiguos que pedem clarificacao), contraexemplos (nao inventar campos) e casos de escalacao humana.

#### Scenario: Catalogo minimo carregado
- **WHEN** o sistema inicializa o catalogo few-shot
- **THEN** existem exemplos dos quatro tipos exigidos disponiveis para montagem do prompt

### Requirement: Sem dados sensiveis reais
Exemplos MUST usar dados ficticios; MUST NOT incluir informacoes sensiveis reais de clientes.

#### Scenario: Revisao de exemplos
- **WHEN** um exemplo e inspecionado no catalogo versionado
- **THEN** nomes, codigos e enderecos sao ficticios/demo

### Requirement: Exemplos injetados na inferencia
Ao executar uma tarefa, o sistema MUST anexar few-shots relevantes ao prompt da tarefa para reforcar estrutura, precisao e limites de inferencia.

#### Scenario: Classificacao com few-shot
- **WHEN** uma mensagem e classificada via biblioteca de prompts
- **THEN** a chamada ao modelo inclui pelo menos um exemplo positivo e um contraexemplo ou caso de clarificacao
