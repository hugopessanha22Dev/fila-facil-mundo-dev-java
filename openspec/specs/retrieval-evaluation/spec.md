# retrieval-evaluation Specification

## Purpose

Medir se a recuperação acha o chunk certo e explicar falha de versão velha, chunk pequeno demais ou filtro incorreto, com o ajuste que passou a valer.

## Requirements

### Requirement: Metricas de recuperacao
O sistema MUST calcular, num conjunto de perguntas com chunk esperado, precision, recall, relevância do chunk recuperado, cobertura das respostas e groundedness.

#### Scenario: Relatorio de avaliacao
- **WHEN** a avaliação roda sobre as perguntas rotuladas
- **THEN** o relatório mostra precision, recall, relevância, cobertura, groundedness e a taxa de respostas sem evidência

### Requirement: Tres classes de falha
O sistema MUST classificar falha de documento desatualizado, falha de chunk pequeno demais e falha de filtro incorreto.

#### Scenario: Versao velha
- **WHEN** a resposta cita a versão anterior de um documento que já tem versão nova
- **THEN** o relatório marca a falha como documento desatualizado

#### Scenario: Chunk curto
- **WHEN** o chunk size corta a regra esperada e o recall daquela pergunta cai
- **THEN** o relatório marca a falha como chunk mal dividido

#### Scenario: Filtro errado
- **WHEN** um filtro de acesso ou domínio exclui o único chunk que responde a pergunta
- **THEN** o relatório marca a falha como filtro incorreto

### Requirement: Ajuste registrado
O sistema MUST registrar, junto da avaliação, o chunk size, o overlap, o top-k, o uso de filtro e o reranking que ficaram ativos depois do diagnóstico.

#### Scenario: Parametro ajustado
- **WHEN** a avaliação termina
- **THEN** o relatório lista os parâmetros vigentes e qual falha cada um evita
