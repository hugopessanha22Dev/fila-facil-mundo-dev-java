# Spec Delta

## Purpose

Medir a triagem num golden dataset de mensagens operacionais ficticias e usar acuracia, extracao, JSON, schema, resumo, custo, latencia, regressao e falhas por categoria como gates de release.

## ADDED Requirements

### Requirement: Golden dataset operacional
O sistema MUST avaliar casos ficticios de consulta, ocorrencia e reclamacao, cada um com a resposta esperada, sem dado real de cliente.

#### Scenario: Tres familias de caso
- **WHEN** a avaliacao offline carrega o dataset da esteira
- **THEN** existem casos de consulta, de ocorrencia e de reclamacao, com saida esperada em cada um

### Requirement: Nove metricas de release
O sistema MUST calcular acuracia de classificacao, extracao dos campos esperados, percentual de JSON valido, aderencia ao schema, presenca de resumo, custo, latencia, regressao contra a linha de base e falhas por categoria.

#### Scenario: Relatorio com as metricas
- **WHEN** a avaliacao offline termina
- **THEN** o relatorio mostra as nove metricas e o total de casos

### Requirement: Gate minimo reprova a release
O sistema MUST reprovar a avaliacao quando JSON valido, aderencia ao schema ou acuracia ficarem abaixo do minimo configurado.

#### Scenario: Schema abaixo do minimo
- **WHEN** a parcela de saidas aderentes ao schema fica abaixo do minimo
- **THEN** a avaliacao termina reprovada e a promocao dessa combinacao e recusada

### Requirement: Regressao ignora variacao irrelevante
O sistema MUST tratar como regressao uma queda de acuracia ou de extracao acima do limite configurado, e MUST NOT reprovar so porque a latencia do mock oscilou dentro do teto absoluto.

#### Scenario: Queda de acuracia
- **WHEN** a acuracia cai alem do limite em relacao a linha de base da mesma familia de casos
- **THEN** a avaliacao e reprovada por regressao

#### Scenario: Latencia ainda dentro do teto
- **WHEN** a latencia media sobe, mas permanece abaixo do teto absoluto de latencia e de custo
- **THEN** a avaliacao nao e reprovada apenas por essa oscilacao
