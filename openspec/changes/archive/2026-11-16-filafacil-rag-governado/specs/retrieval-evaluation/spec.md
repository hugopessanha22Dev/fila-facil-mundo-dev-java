# Spec Delta

## ADDED Requirements

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
