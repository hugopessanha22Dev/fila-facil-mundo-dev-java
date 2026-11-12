# Spec Delta

## Purpose

Registrar por que a regra de transição ficou na entidade e por que o vocabulário dos contextos vizinhos passa por tradução.

## ADDED Requirements

### Requirement: Decisao comparada
O sistema MUST guardar um documento de decisão que compare modelo anêmico com entidade rica e vocabulário único com contextos traduzidos, e MUST nomear a opção adotada em cada comparação.

#### Scenario: Documento presente
- **WHEN** o documento de decisão é lido
- **THEN** ele contém as duas comparações, a opção adotada e a consequência de não colocar regra de domínio no adaptador

### Requirement: Evolucao sem reescrever o nucleo
O documento MUST descrever a versão inicial em que a transição ficaria no comando de entrada, e a versão adotada em que a mesma transição ficou na entidade, sem mudar o contrato dos casos de uso.

#### Scenario: Contrato preservado
- **WHEN** a decisão descreve a evolução
- **THEN** os casos de uso continuam sendo abrir, analisar, escalar, resolver, cancelar e consultar
