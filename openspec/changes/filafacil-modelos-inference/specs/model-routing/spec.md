# Spec Delta

## Purpose

Escolhe automaticamente entre perfil de modelo leve (triagem barata/rápida) e perfil detalhado (casos complexos ou críticos), com parâmetros de inferência por cenário e sem decisão manual do operador.

## ADDED Requirements

### Requirement: Dois perfis comparáveis
O sistema MUST configurar pelo menos dois perfis de modelo distintos (leve e detalhado) documentando dimensões de comparação: janela de contexto, latência esperada, custo estimado e qualidade relativa.

#### Scenario: Documentação de perfis
- **WHEN** o operador consulta o README do módulo de inferência
- **THEN** encontra a comparação dos dois perfis nas dimensões exigidas

### Requirement: Roteamento automático
O roteador MUST selecionar o perfil com base em regras observáveis (tipo da tarefa, criticidade, tamanho da entrada ou necessidade de qualidade), sem escolha manual no fluxo padrão.

#### Scenario: Entrada crítica longa
- **WHEN** a mensagem indica reclamação crítica ou texto longo/ambíguo
- **THEN** o roteador seleciona o perfil detalhado

#### Scenario: Classificação simples
- **WHEN** a mensagem é curta e de baixa criticidade (ex.: consulta simples de status)
- **THEN** o roteador seleciona o perfil leve

### Requirement: Parâmetros por cenário
Cada perfil/cenário MUST definir temperature, top-p, max tokens e, quando suportado, seed/stop; casos críticos MUST priorizar previsibilidade (temperature baixa).

#### Scenario: Caso crítico
- **WHEN** o roteador envia um caso marcado como crítico ao gateway
- **THEN** os parâmetros aplicados usam temperature baixa para maior previsibilidade
