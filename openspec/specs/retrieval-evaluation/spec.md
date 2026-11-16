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

### Requirement: Documento expirado na resposta
O sistema MUST marcar a falha quando uma resposta operacional cita documento expirado e MUST registrar a ação de tirá-lo da busca ativa.

#### Scenario: Citacao expirada
- **WHEN** a resposta usa um chunk de documento expirado
- **THEN** o relatório marca documento expirado e a ação de bloqueio na busca operacional

### Requirement: Top-k insuficiente
O sistema MUST marcar a falha quando o top-k deixa de fora o chunk esperado de uma pergunta multi-documento e MUST registrar a ação de ampliar o teto.

#### Scenario: Chunk esperado de fora
- **WHEN** o chunk rotulado só aparece depois do top-k simples
- **THEN** o relatório marca top-k insuficiente e a ação de ampliar o teto

### Requirement: Expansao inadequada
O sistema MUST marcar a falha quando a expansão troca o domínio da pergunta e MUST registrar a ação de manter os termos originais.

#### Scenario: Dominio trocado
- **WHEN** a expansão substitui o domínio pedido por outro
- **THEN** o relatório marca expansão inadequada e a ação de preservar a intenção

### Requirement: Reranking fraco
O sistema MUST marcar a falha quando um chunk vigente e pertinente fica atrás de um chunk genérico e MUST registrar a ação de religar o reranking.

#### Scenario: Vigente atras
- **WHEN** o reranking está desligado e o chunk da seção pedida não lidera
- **THEN** o relatório marca reranking fraco e a ação de religá-lo
