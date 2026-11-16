# Spec Delta

## ADDED Requirements

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
