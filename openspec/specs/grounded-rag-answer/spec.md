# grounded-rag-answer Specification

## Purpose

Responder ao operador só com regra recuperada do índice, citando documento, versão e seção, e dizendo quando a base não tem a resposta.

## Requirements

### Requirement: Intencao direciona a busca
O sistema MUST identificar o domínio operacional da pergunta e usá-lo como filtro quando ele for reconhecível.

#### Scenario: Pergunta de atraso
- **WHEN** o operador pergunta sobre procedimento de atraso
- **THEN** a busca prioriza o domínio de atraso e a resposta cita esse domínio

### Requirement: Resposta com citacao
O sistema MUST citar, em toda regra operacional da resposta, o documento, a versão e a seção do chunk usado, e MUST separar o trecho documental da orientação sugerida.

#### Scenario: Pergunta objetiva
- **WHEN** a pergunta tem resposta direta num único chunk vigente
- **THEN** a resposta inclui o trecho e a citação com documento, versão e seção

### Requirement: Sem evidencia nao ha regra
O sistema MUST informar que a resposta não está nos documentos quando nenhum chunk passar no corte, e MUST NOT completar a regra com texto inventado.

#### Scenario: Tema ausente
- **WHEN** a pergunta não aparece em nenhum documento indexado
- **THEN** a resposta declara ausência de evidência, não cita fonte e não afirma uma regra operacional

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
