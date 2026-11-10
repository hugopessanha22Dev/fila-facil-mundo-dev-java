# Spec Delta

## Purpose

Valida manifestos locais de carga (JSON ou CSV) no terminal, verificando campos obrigatorios e inconsistencias que impediriam a saida da rota.

## ADDED Requirements

### Requirement: Leitura de manifesto local
O sistema MUST ler um manifesto a partir de arquivo local em JSON ou CSV sem exigir banco, API ou interface web.

#### Scenario: Manifesto JSON valido
- **WHEN** o operador informa o caminho de um manifesto JSON existente
- **THEN** o sistema carrega os registros e inicia a validacao

#### Scenario: Arquivo inexistente
- **WHEN** o caminho informado nao existe
- **THEN** o sistema reporta erro tipado e nao encerra o processo de forma abrupta sem mensagem

### Requirement: Campos obrigatorios
Cada registro MUST conter pacote, rota, veiculo, responsavel e status; campos vazios ou ausentes MUST gerar erro de validacao.

#### Scenario: Registro sem responsavel
- **WHEN** um registro nao possui responsavel preenchido
- **THEN** a validacao inclui erro identificando o registro e o campo ausente

### Requirement: Deteccao de inconsistencias
O sistema MUST detectar pacotes duplicados, rotas vazias e registros sem veiculo ou sem responsavel.

#### Scenario: Pacote duplicado
- **WHEN** dois registros compartilham o mesmo identificador de pacote
- **THEN** a validacao reporta erro de duplicidade para esse pacote
