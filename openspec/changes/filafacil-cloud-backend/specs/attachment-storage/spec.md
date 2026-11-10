# Spec Delta

## Purpose

Armazenar anexos e comprovantes de agendamento em storage cloud, mantendo no banco apenas a referencia do objeto.

## ADDED Requirements

### Requirement: Upload de anexo em storage cloud
A API MUST permitir associar um anexo de agendamento ao storage cloud configurado (sem exigir arquivos grandes).

#### Scenario: Anexo enviado
- **WHEN** o cliente envia um anexo pequeno valido para um agendamento existente
- **THEN** o arquivo e armazenado no storage cloud e a operacao conclui com sucesso

### Requirement: Referencia no banco
O banco MUST armazenar apenas a referencia (chave/URL logica) do objeto no storage, nao o conteudo binario do arquivo.

#### Scenario: Consulta apos upload
- **WHEN** um agendamento com anexo e consultado
- **THEN** a resposta inclui a referencia do objeto no storage e nao o binario completo
