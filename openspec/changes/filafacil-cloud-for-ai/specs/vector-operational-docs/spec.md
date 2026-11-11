# Spec Delta

## Purpose

Guardar documentos operacionais ficticios da FilaFacil em um banco vetorial versionado, para a triagem buscar contexto por similaridade sem inventar politica e sem usar dado real de cliente.

## ADDED Requirements

### Requirement: Quatro familias de documentos
O sistema MUST indexar somente documentos ficticios das familias regras de coleta, politica de entrega, tipos de ocorrencia e instrucoes operacionais.

#### Scenario: Documento de cada familia
- **WHEN** a base e carregada
- **THEN** existe ao menos um documento de cada uma das quatro familias, identificavel pela familia e sem dado real de cliente

#### Scenario: Documento fora das familias
- **WHEN** um item nao pertence a nenhuma das quatro familias
- **THEN** o sistema recusa a indexacao e informa a familia invalida

### Requirement: Embeddings organizados por versao
O sistema MUST armazenar embeddings separados por ambiente, dominio e versao, de forma que uma versao antiga permaneca consultavel para reprocessamento.

#### Scenario: Nova versao nao apaga a anterior
- **WHEN** uma nova versao de embeddings e publicada
- **THEN** a versao anterior continua enderecavel e a versao ativa da triagem passa a ser a nova

#### Scenario: Reprocessar uma versao
- **WHEN** um operador pede reprocessamento de uma versao especifica
- **THEN** o sistema regenera os embeddings daquela versao sem misturar vetores de outra versao ou de outro ambiente

### Requirement: Busca por similaridade
O sistema MUST devolver os trechos mais proximos da mensagem, com a familia do documento, a versao e um indicador de similaridade.

#### Scenario: Contexto encontrado
- **WHEN** a mensagem se aproxima de um documento indexado na versao ativa
- **THEN** a busca retorna esse documento com familia, versao e similaridade, para a resposta citar a origem

#### Scenario: Nenhum documento proximo
- **WHEN** nenhum documento da versao ativa fica acima do corte de similaridade
- **THEN** a busca devolve conjunto vazio e a triagem marca o contexto como ausente, sem inventar politica
