# Spec Delta

## Purpose

Garante grounding e qualidade das respostas de IA: rubricas de validacao, proibicao de inventar dados, rastreabilidade fato/inferencia/ausente e retry orientado ao erro sem loop infinito.

## ADDED Requirements

### Requirement: Rubricas de validacao
Cada resposta MUST ser avaliada quanto a formato correto, campos obrigatorios, coerencia com a mensagem/historico, ausencia de informacoes inventadas e proxima acao adequada.

#### Scenario: Formato invalido
- **WHEN** a saida falha na rubrica de formato
- **THEN** a resposta e rejeitada e um retry orientado ao erro de formato pode ser disparado

### Requirement: Anti-alucinacao / grounding
O modelo MUST usar apenas informacoes da mensagem, do historico da conversa ou de fontes explicitamente disponibilizadas; MUST NOT inventar status, prazo, responsavel, codigo ou causa.

#### Scenario: Entrada vaga sem status
- **WHEN** a mensagem nao informa status da entrega
- **THEN** a saida marca status como ausente/desconhecido e nao inventa um valor

### Requirement: Diferenciar fato, inferencia e ausente
A saida estruturada MUST distinguir fato informado, inferencia provavel (quando permitida e evidenciada) e informacao ausente.

#### Scenario: Campo com fonte
- **WHEN** o codigo de entrega aparece literalmente na mensagem
- **THEN** a saida registra o valor como fato com rastreabilidade a entrada

### Requirement: Retry limitado e orientado
Falhas de validacao MUST acionar retry com instrucao especifica do erro, com limite maximo de tentativas, sem loop infinito.

#### Scenario: Retry esgotado
- **WHEN** as tentativas de retry orientado se esgotam ainda com falha de rubrica
- **THEN** o sistema rejeita a saida, registra o erro e indica revisao humana
