# Spec Delta

## ADDED Requirements

### Requirement: Compressao contextual
O sistema MUST remover do contexto um trecho redundante cujo texto já está num trecho melhor ranqueado, e MUST manter o trecho que contém a evidência distintiva da pergunta.

#### Scenario: Trecho repetido
- **WHEN** dois chunks trazem o mesmo parágrafo e um deles está à frente no ranking
- **THEN** o contexto enviado à resposta fica só com o primeiro

#### Scenario: Evidencia distintiva
- **WHEN** um chunk menor contém o termo que a pergunta pede e um chunk maior não contém
- **THEN** a compressão conserva o chunk menor

### Requirement: Top-k dinamico
O sistema MUST usar o top-k configurado numa pergunta de um único domínio e MUST ampliá-lo, até o teto configurado, quando a pergunta pede evidência de mais de um documento.

#### Scenario: Pergunta simples
- **WHEN** a pergunta cabe num único domínio
- **THEN** o número de chunks não passa do top-k configurado

#### Scenario: Pergunta multi-documento
- **WHEN** a pergunta pede dois domínios com relação clara
- **THEN** o contexto pode reunir chunks dos dois, sem passar do teto
