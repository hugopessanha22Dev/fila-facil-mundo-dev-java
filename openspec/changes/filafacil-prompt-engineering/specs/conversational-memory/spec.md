# Spec Delta

## Purpose

Conduz atendimento operacional em multiplos turnos, guardando campos ja informados e pedindo clarificacao apenas do que ainda falta, ate gerar conclusao estruturada.

## ADDED Requirements

### Requirement: Memoria de campos coletados
O sistema MUST manter memoria de contexto da sessao com intencao identificada e campos ja fornecidos pelo usuario ao longo dos turnos.

#### Scenario: Segundo turno com codigo informado
- **WHEN** o usuario informa o codigo de entrega apos uma pergunta de clarificacao
- **THEN** a memoria da sessao passa a conter esse codigo sem descartar campos anteriores

### Requirement: Clarificacao incremental
O sistema MUST formular perguntas apenas para campos obrigatorios ainda ausentes e MUST NOT repetir perguntas ja respondidas na mesma sessao.

#### Scenario: Campo ja respondido
- **WHEN** o cliente ja informou o nome e ainda falta o prazo
- **THEN** a proxima pergunta trata do prazo e nao solicita o nome novamente

### Requirement: Ambientes de mensagem cobertos
O fluxo MUST aceitar mensagens de cliente, operador, parceiro e motorista, ajudando a entender, extrair, resumir, classificar e indicar proxima acao.

#### Scenario: Motorista reporta endereco
- **WHEN** o motorista descreve problema no endereco de entrega em texto livre
- **THEN** o fluxo inicia/atualiza a sessao visando registro padronizado e eventual encaminhamento humano

### Requirement: Conclusao so com campos resolvidos
O sistema MUST gerar saida estruturada final somente quando campos obrigatorios estiverem preenchidos ou explicitamente marcados como desconhecidos com justificativa.

#### Scenario: Lacunas pendentes
- **WHEN** ainda faltam campos obrigatorios sem marcacao de desconhecido
- **THEN** o sistema nao emite conclusao final e solicita clarificacao
