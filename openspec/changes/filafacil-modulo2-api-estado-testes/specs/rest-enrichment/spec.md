# Spec Delta

## Purpose

Define o enriquecimento assincrono do cadastro de coletas via API REST externa (CEP), incluindo sucesso com dados enriquecidos e falha tratada sem derrubar o sistema.

## ADDED Requirements

### Requirement: Consultar API REST no cadastro
Ao cadastrar uma coleta, o sistema MUST consultar uma API REST externa (CEP) de forma assincrona e MUST NOT finalizar o cadastro antes de receber a resposta ou tratar a falha.

#### Scenario: Cadastro aguarda resposta da API
- **WHEN** o usuario confirma um cadastro com CEP valido
- **THEN** o sistema MUST aguardar a conclusao da chamada assincrona antes de persistir a coleta como concluida no fluxo de cadastro

### Requirement: Enriquecer coleta com JSON da API
O sistema MUST interpretar a resposta JSON da API e enriquecer a coleta com dados derivados (ex.: bairro, cidade e UF), persistindo esses campos junto aos dados basicos.

#### Scenario: CEP encontrado
- **WHEN** a API retorna dados validos para o CEP informado
- **THEN** a coleta cadastrada MUST conter os campos enriquecidos preenchidos a partir do JSON

### Requirement: Falha de API nao encerra o sistema
Se a API falhar (rede, CEP invalido, timeout ou resposta de erro), o sistema MUST tratar a falha, informar o usuario e MUST NOT encerrar a aplicacao.

#### Scenario: CEP inexistente ou API indisponivel
- **WHEN** a consulta REST falha durante o cadastro
- **THEN** o sistema MUST exibir mensagem clara de erro de API e MUST permanecer no menu operacional
