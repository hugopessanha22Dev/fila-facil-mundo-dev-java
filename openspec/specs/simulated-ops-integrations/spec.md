# simulated-ops-integrations Specification

## Purpose

Oferecer quatro integrações locais, com contrato e erro simulável, para o agente consultar, gravar, notificar e fundamentar a decisão.

## Requirements

### Requirement: API simulada de coletas e entregas
O sistema MUST expor consulta de coleta e entrega com resposta estruturada e MUST permitir marcar a consulta como indisponível num cenário de teste.

#### Scenario: Consulta indisponivel
- **WHEN** o cenário liga a falha da API de consulta
- **THEN** a ferramenta devolve erro de indisponibilidade e o agente não trata o caso como encontrado

### Requirement: Banco de clientes e ocorrencias
O sistema MUST ler cliente e gravar ocorrência em store local, e MUST simular erro de escrita sem persistir o registro.

#### Scenario: Erro ao gravar
- **WHEN** o cenário liga a falha de registro
- **THEN** a ocorrência não aparece no store e o retorno traz o erro

### Requirement: Fila de notificacoes
O sistema MUST enfileirar notificação e encaminhamento com payload, instante e resultado.

#### Scenario: Notificacao aceita
- **WHEN** o agente notifica com código e mensagem válidos
- **THEN** a fila guarda payload, instante e resultado de sucesso

### Requirement: Base de regras consultavel
O sistema MUST responder a consulta de regra com o trecho e a citação da base documental já indexada, ou com ausência quando a regra não existir.

#### Scenario: Regra encontrada
- **WHEN** o agente pergunta uma regra presente na base
- **THEN** o retorno inclui o trecho e a citação, e o trace aponta essa ferramenta
