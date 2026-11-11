# Spec Delta

## Purpose

Delimitar o que o agente operacional pode fazer, quantos passos ele aguenta e em que condição ele encerra ou escala para um humano.

## ADDED Requirements

### Requirement: Objetivo logistico explicito
O sistema MUST declarar o objetivo de tratar atraso, endereço incorreto, falha de coleta e ocorrência crítica, e MUST listar as ferramentas permitidas para esse objetivo.

#### Scenario: Pedido dentro do dominio
- **WHEN** a mensagem descreve atraso, endereço, falha de coleta ou ocorrência
- **THEN** o agente aceita o caso e só oferece ferramentas da lista operacional

#### Scenario: Pedido fora do dominio
- **WHEN** a mensagem pede uma ação que não é logística
- **THEN** o agente recusa sem chamar ferramenta e registra o motivo

### Requirement: Limites de execucao
O sistema MUST impor um máximo de passos por fluxo, um máximo de tentativas por ferramenta e um máximo de chamadas por execução.

#### Scenario: Teto de passos
- **WHEN** o fluxo atinge o número máximo de passos sem resolução
- **THEN** o agente encerra com status de limite e não faz nova chamada

### Requirement: Criterios de parada
O sistema MUST encerrar ao resolver o caso, ao faltar dado essencial, ao repetir falha da mesma ferramenta ou ao atingir um limite, e MUST escalar quando o caso for incerto ou crítico.

#### Scenario: Dado essencial ausente
- **WHEN** a mensagem não traz código nem cliente e a ferramenta exige um dos dois
- **THEN** o agente para e pede o dado, sem registrar ocorrência nem notificar

#### Scenario: Caso critico
- **WHEN** a mensagem é ambígua ou descreve risco operacional
- **THEN** o agente encerra encaminhando para atendimento humano com o motivo
