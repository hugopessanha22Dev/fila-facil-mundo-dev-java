# Spec Delta

## Purpose

Executar o ciclo da exceção com repositório, publicador e gateway simulados, por uma CLI local, sem banco, API obrigatória ou fila real.

## ADDED Requirements

### Requirement: Adaptadores em memoria
O sistema MUST guardar exceções e eventos publicados em adaptadores de memória, e MUST devolver a exceção salva na consulta feita na mesma execução.

#### Scenario: Evento publicado
- **WHEN** a análise é aceita
- **THEN** o publicador em memória contém o evento dessa transição e o repositório devolve a exceção em análise

### Requirement: CLI do orquestrador
O sistema MUST aceitar, por linha de comando, abrir, analisar, escalar, resolver, cancelar e consultar, e MUST manter o menu do terminal nas opções 0 a 12.

#### Scenario: Consulta pela CLI
- **WHEN** a CLI abre uma exceção e em seguida consulta o identificador
- **THEN** a saída mostra status aberta, código da entrega e tipo

#### Scenario: Menu intacto
- **WHEN** o código do menu é inspecionado
- **THEN** a opção 12 continua presente e não existe opção 13

### Requirement: Falha sem derrubar o processo
O sistema MUST informar recusa de transição ou termo desconhecido e MUST continuar a execução, sem encerrar por essa recusa.

#### Scenario: Comando recusado
- **WHEN** a CLI pede resolução de uma exceção ainda aberta
- **THEN** a saída explica a recusa e o processo devolve código de comando tratado
