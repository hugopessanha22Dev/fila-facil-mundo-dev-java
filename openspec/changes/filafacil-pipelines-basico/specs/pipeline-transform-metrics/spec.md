# Spec Delta

## Purpose

Normaliza, limpa e cruza as fontes ingestadas para produzir modelo consolidado e metricas operacionais diarias da FilaFácil.

## ADDED Requirements

### Requirement: Normalizacao de campos
O sistema SHALL padronizar nomes de campos, status, regioes, identificadores e formatos de data antes de joins e agregacoes.

#### Scenario: Status e datas uniformizados
- **WHEN** fontes brutas trazem status/regiao/data em formatos divergentes
- **THEN** a camada processada expoe valores normalizados com convencao unica documentada

### Requirement: Filtros e limpeza com rastreio
O sistema SHALL remover registros invalidos, incompletos ou fora do padrao antes de joins/agregacoes e MUST contabilizar registros descartados (sem perda silenciosa).

#### Scenario: Registro incompleto descartado e contado
- **WHEN** um registro bruta nao possui campos obrigatorios
- **THEN** o registro nao entra na camada processada e a contagem de descartados aumenta

### Requirement: Joins com cadastro relacional
O sistema SHALL cruzar operacoes (coletas, entregas e ocorrencias) com a tabela de clientes/unidades para enriquecimento.

#### Scenario: Entrega enriquecida com unidade
- **WHEN** uma entrega referencia um cliente presente no cadastro
- **THEN** o registro consolidado inclui dados de unidade/regiao vindos do cadastro

### Requirement: Agregacoes e metricas operacionais
O sistema SHALL produzir agregacoes e metricas simples por data, regiao, status e cliente, incluindo totais de coletas, entregas concluidas, atrasos e ocorrencias por regiao.

#### Scenario: Resumo diario por regiao
- **WHEN** a etapa de transformacao conclui com dados processados validos
- **THEN** a camada consolidada contem metricas agregadas consultaveis por data e regiao
