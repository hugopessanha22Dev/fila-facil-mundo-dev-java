# Spec Delta

## Purpose

Camada única de serviço que executa chamadas a modelos de IA com autenticação, timeout, retry, fallback, log e estimativa de custo, padronizando erros e respostas para o restante do FilaFácil.

## ADDED Requirements

### Requirement: Chamadas passam apenas pelo gateway
O sistema MUST executar toda inferência através de uma única camada de serviço de gateway; nenhuma outra camada MUST chamar o provedor de modelo diretamente.

#### Scenario: Menu solicita triagem
- **WHEN** o operador dispara uma triagem a partir do menu
- **THEN** a execução da chamada ao modelo ocorre somente via gateway de inferência

### Requirement: Autenticação por ambiente
O gateway MUST obter credenciais de API apenas de variáveis de ambiente (ou equivalente local não versionado) e MUST NOT embutir chaves no código ou em arquivos commitados.

#### Scenario: Chave ausente
- **WHEN** a variável de ambiente da chave de API não está definida e o modo não é mock
- **THEN** o gateway falha com erro tipado de configuração e a aplicação permanece em execução

### Requirement: Timeout e retries limitados
O gateway MUST aplicar timeout configurável por chamada e MUST repetir falhas transitórias no máximo N vezes (N configurável, padrão baixo), sem retries infinitos.

#### Scenario: Timeout esgotado
- **WHEN** o provedor não responde dentro do timeout e os retries se esgotam
- **THEN** o gateway retorna erro tipado de rede/timeout sem encerrar a aplicação

### Requirement: Fallback de modelo
Quando o modelo primário falhar de forma recuperável, o gateway MUST tentar o modelo/perfil de fallback configurado antes de rejeitar a operação.

#### Scenario: Primário falha e fallback sucede
- **WHEN** a chamada ao modelo primário falha com erro recuperável e o fallback está configurado
- **THEN** o gateway executa o perfil de fallback e, se válido, retorna essa resposta

### Requirement: Log e custo por chamada
O gateway MUST registrar log básico de cada tentativa (perfil, latência, sucesso/falha) e MUST expor estimativa de custo associado à chamada quando o provedor ou a tabela local permitir.

#### Scenario: Chamada bem-sucedida
- **WHEN** uma inferência completa com sucesso
- **THEN** o log inclui perfil usado, latência e custo estimado (ou "indisponível" se não houver tabela)

### Requirement: Modo mock para testes
O gateway MUST oferecer modo mock que não realiza chamada de rede, produzindo resposta controlada para avaliação e testes automatizados.

#### Scenario: Teste sem rede
- **WHEN** o gateway está em modo mock
- **THEN** nenhuma requisição HTTP ao provedor é disparada
