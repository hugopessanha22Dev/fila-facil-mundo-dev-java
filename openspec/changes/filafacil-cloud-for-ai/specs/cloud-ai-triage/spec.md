# Spec Delta

## Purpose

Processar uma solicitacao simulada de triagem logistica na cloud: entra por fila, passa por validacao e modelo, devolve resposta estruturada ao operador e registra falhas de modelo sem derrubar a plataforma.

## ADDED Requirements

### Requirement: Entrada assincrona por fila
O sistema MUST aceitar mensagens simuladas de cliente, operador ou parceiro por uma fila, devolvendo um identificador de solicitacao antes do fim do processamento.

#### Scenario: Mensagem aceita na fila
- **WHEN** uma mensagem simulada valida e publicada na fila do ambiente
- **THEN** o sistema confirma o aceite com um identificador e processa a solicitacao fora da chamada de entrada

#### Scenario: Fila registra o percurso
- **WHEN** uma solicitacao e consumida da fila
- **THEN** o sistema registra o identificador, o ambiente e o estado (aceita, em processamento, concluida ou falha)

### Requirement: Validacao de payload antes do modelo
O sistema MUST validar o payload e recusar o processamento de modelo quando a mensagem estiver vazia ou sem os campos minimos da solicitacao.

#### Scenario: Payload incompleto
- **WHEN** a mensagem chega sem texto de solicitacao
- **THEN** o sistema marca a solicitacao como rejeitada, nao chama a API de modelo e registra o motivo

#### Scenario: Payload valido segue
- **WHEN** a mensagem tem texto de solicitacao e origem simulada
- **THEN** o sistema libera a solicitacao para classificacao

### Requirement: Classificacao e extracao operacional
O sistema MUST classificar o tipo de solicitacao e extrair os dados operacionais presentes no texto por meio de uma API de modelo gerenciada.

#### Scenario: Classificacao concluida
- **WHEN** o modelo responde dentro do timeout e do teto de tokens
- **THEN** o resultado contem o tipo de solicitacao e os campos extraidos, ou a marca explicita do que nao foi encontrado

### Requirement: Resposta estruturada ao operador
O sistema MUST devolver ao operador uma resposta estruturada com a classificacao, a proxima acao e o contexto usado, sem incluir segredo ou dado sensivel.

#### Scenario: Resposta pronta
- **WHEN** a classificacao e o contexto terminam com sucesso
- **THEN** o operador recebe um objeto estruturado com tipo, resumo, proxima acao e identificador da inferencia

### Requirement: Cache de consulta frequente
O sistema MUST reutilizar resposta em cache apenas quando a consulta, a versao de prompt e a versao dos documentos forem as mesmas e o prazo de validade nao tiver vencido.

#### Scenario: Consulta repetida dentro da validade
- **WHEN** a mesma consulta chega de novo antes do vencimento, com o mesmo prompt e a mesma versao de documentos
- **THEN** o sistema devolve a resposta em cache e registra que nao houve nova chamada de modelo

#### Scenario: Cache vencido ou versao diferente
- **WHEN** a validade expirou ou a versao de prompt ou de documentos mudou
- **THEN** o sistema ignora o cache, chama o modelo e grava uma resposta nova

### Requirement: Falha de modelo nao perde a solicitacao
O sistema MUST tratar timeout, limite de taxa e erro do modelo, preservando a solicitacao com status de erro e sem repetir chamadas alem do limite configurado.

#### Scenario: Timeout do modelo
- **WHEN** a API de modelo nao responde dentro do timeout do ambiente
- **THEN** o sistema encerra a tentativa, marca erro de timeout e nao deixa a solicitacao em processamento eterno

#### Scenario: Limite de taxa
- **WHEN** a API de modelo recusa por excesso de chamadas
- **THEN** o sistema registra o limite atingido e nao continua chamando alem do teto de tentativas
