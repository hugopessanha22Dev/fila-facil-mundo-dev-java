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

### Requirement: Historico operacional
O sistema MUST ingerir um documento fictício de histórico operacional de decisões, além das sete famílias já exigidas.

#### Scenario: Oitava familia
- **WHEN** a ingestão carrega o corpus padrão
- **THEN** o histórico operacional aparece com título, tipo, domínio, versão, data, owner, nível de acesso e texto

### Requirement: Status do documento
O sistema MUST registrar o status vigente, expirado, substituído ou em revisão, e MUST manter o substituído num namespace histórico, fora da busca operacional.

#### Scenario: Documento substituido
- **WHEN** uma versão nova substitui a anterior
- **THEN** a versão anterior fica no namespace histórico e a busca operacional devolve a versão vigente

### Requirement: Hash e linhagem
O sistema MUST guardar o hash do conteúdo e a relação com a versão anterior ou posterior.

#### Scenario: Mesmo texto
- **WHEN** o conteúdo de um documento não muda
- **THEN** o hash permanece o mesmo e a linhagem continua apontando a versão vizinha

### Requirement: Execucao de ingestao
O sistema MUST registrar cada execução de ingestão e MUST reprocessar apenas os chunks do documento alterado.

#### Scenario: Um documento muda
- **WHEN** só o histórico operacional é republicado
- **THEN** o registro da execução cita esse documento e os vetores dos demais documentos permanecem
