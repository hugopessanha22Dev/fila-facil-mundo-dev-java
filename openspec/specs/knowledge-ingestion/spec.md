# knowledge-ingestion Specification

## Purpose

Transformar documentos operacionais fictícios em chunks limpos, sem duplicata, com metadados e com troca de versão que não reindexa o restante da base.

## Requirements

### Requirement: Corpus operacional simulado
O sistema MUST ingerir documentos fictícios de política de coletas, política de entregas, manual de ocorrências, FAQ de atendimento, regras de comprovantes, procedimento de atraso e instruções para parceiros.

#### Scenario: Sete familias presentes
- **WHEN** a ingestão carrega o corpus padrão
- **THEN** cada uma das sete famílias aparece com título, tipo, domínio, versão, data, owner, nível de acesso e texto

### Requirement: Chunk com rastreio
O sistema MUST partir o texto em chunks que conservam documento, versão, seção, domínio, data de atualização e nível de acesso.

#### Scenario: Chunk aponta a origem
- **WHEN** um documento com duas seções é ingerido
- **THEN** cada chunk informa o id do documento, a versão, a seção e o nível de acesso do original

### Requirement: Limpeza e deduplicacao
O sistema MUST descartar chunk vazio e MUST NOT indexar duas vezes o mesmo texto da mesma versão e seção.

#### Scenario: Texto repetido
- **WHEN** o mesmo parágrafo da mesma versão entra duas vezes
- **THEN** o índice fica com um único chunk desse parágrafo

### Requirement: Atualizacao incremental
O sistema MUST substituir os chunks da versão anterior de um documento quando uma versão nova é publicada, e MUST manter os chunks dos demais documentos.

#### Scenario: Nova versao de um documento
- **WHEN** a política de entregas passa da versão 1 para a versão 2
- **THEN** a busca ativa deixa de devolver chunks da versão 1 desse documento e continua devolvendo os outros documentos já indexados
