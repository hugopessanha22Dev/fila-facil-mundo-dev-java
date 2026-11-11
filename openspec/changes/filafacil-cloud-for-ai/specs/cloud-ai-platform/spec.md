# Spec Delta

## Purpose

Sustentar o assistente numa plataforma cloud de um unico provedor, com compute, fila, serverless, cache, rede minima, deploy reproduzivel e custo visivel por ambiente.

## ADDED Requirements

### Requirement: Compute continuo separado do serverless
O sistema MUST manter a aplicacao principal em compute continuo e MUST executar validacao de payload, limpeza de prompt e pos-processamento da resposta em funcao serverless.

#### Scenario: Validacao fora do processo principal
- **WHEN** uma mensagem entra na plataforma
- **THEN** a checagem do payload ocorre na funcao serverless antes de a aplicacao principal chamar o modelo

#### Scenario: Pos-processamento serverless
- **WHEN** o modelo devolve uma resposta
- **THEN** a funcao serverless limpa o prompt registrado e normaliza a resposta antes do armazenamento

### Requirement: Entrada com balanceador e escala
O sistema MUST publicar a aplicacao atras de um balanceador de carga e MUST escalar sozinha dentro de limites minimo e maximo diante de aumento moderado de carga, sem ajuste manual de instancia.

#### Scenario: Deploy reproduzivel
- **WHEN** o mesmo pacote de deploy e aplicado de novo num ambiente
- **THEN** a topologia resultante (compute, balanceador, limites de escala) coincide com a declarada para aquele ambiente

#### Scenario: Carga moderada
- **WHEN** a carga sobe dentro da faixa prevista
- **THEN** o numero de instancias varia entre o minimo e o maximo configurados sem intervencao manual

### Requirement: Permissao minima e rede controlada
O sistema MUST conceder a cada servico apenas as permissoes necessarias ao seu prefixo de storage, fila, cofre e logs, e MUST NOT usar permissao de administrador.

#### Scenario: Servico de inferencia
- **WHEN** o papel do servico de inferencia e avaliado
- **THEN** ele le o secret do proprio ambiente, publica resultado no prefixo de inferencia e escreve log, sem permissao de administracao da conta

### Requirement: Recursos identificados e com custo
O sistema MUST recusar a declaracao de recurso cloud sem tag de ambiente, owner e finalidade, e MUST expor uso de tokens, chamadas, latencia, taxa de erro, compute, storage, fila e custo estimado por ambiente.

#### Scenario: Recurso sem tag
- **WHEN** um recurso da plataforma e declarado sem ambiente, owner ou finalidade
- **THEN** a definicao e considerada invalida e nao entra no deploy daquele ambiente

#### Scenario: Painel minimo por ambiente
- **WHEN** um operador consulta a observabilidade de um ambiente
- **THEN** ele ve tokens, chamadas, latencia, taxa de erro, compute, storage, profundidade da fila e custo estimado daquele ambiente

### Requirement: Alerta basico de consumo
O sistema MUST emitir alerta quando a taxa de erro, a fila ou o consumo de tokens ultrapassar o limiar do ambiente.

#### Scenario: Tokens acima do limiar
- **WHEN** o consumo de tokens do periodo passa do limiar configurado
- **THEN** o sistema registra um alerta associado ao ambiente, sem incluir conteudo de mensagem de cliente

### Requirement: Escopo fechado de cloud para IA
O sistema MUST NOT treinar modelo proprio, MUST NOT executar fine-tuning, MUST NOT usar dado real de cliente e MUST NOT provisionar um segundo provedor cloud.

#### Scenario: Pedido de treino
- **WHEN** uma operacao pede treinamento ou fine-tuning
- **THEN** a plataforma recusa a operacao e mantem apenas consumo de API de modelo gerenciada
