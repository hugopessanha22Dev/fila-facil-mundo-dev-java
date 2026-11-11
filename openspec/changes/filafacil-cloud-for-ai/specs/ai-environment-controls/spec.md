# Spec Delta

## Purpose

Separar desenvolvimento, homologacao e producao da plataforma de IA, com secrets, quotas e rate limit proprios, para experimento local nao usar a mesma chave nem o mesmo teto da producao.

## ADDED Requirements

### Requirement: Tres ambientes segregados
O sistema MUST operar com os ambientes desenvolvimento, homologacao e producao, cada um com variaveis, credenciais e quotas proprias.

#### Scenario: Ambiente de desenvolvimento
- **WHEN** a plataforma sobe em desenvolvimento
- **THEN** ela usa secrets, quota permissiva e permissao de desenvolvimento, sem reutilizar credencial de producao

#### Scenario: Homologacao espelha producao com teto intermediario
- **WHEN** a plataforma sobe em homologacao
- **THEN** a topologia de servicos segue a de producao, com quota intermediaria, rate limit ativo e credencial diferente da de desenvolvimento

#### Scenario: Producao mais restrita
- **WHEN** a plataforma sobe em producao
- **THEN** quotas de tokens, compute e storage e o rate limit ficam mais restritos que em desenvolvimento

### Requirement: Secrets fora do codigo
O sistema MUST ler chaves de API de modelo de um cofre de secrets do ambiente, com acesso minimo pelo servico que chama o modelo. O repositorio MUST NOT conter chave real.

#### Scenario: Chamada com secret do ambiente
- **WHEN** o servico de inferencia precisa da chave
- **THEN** ele a obtem do cofre daquele ambiente e o valor nao aparece em codigo, log ou resultado

#### Scenario: Rotacao em producao
- **WHEN** o secret de producao e rotacionado no cofre
- **THEN** novas chamadas passam a usar o secret novo sem alterar o codigo

### Requirement: Quota por ambiente
O sistema MUST impedir que um ambiente ultrapasse a quota configurada de tokens, chamadas, compute ou storage.

#### Scenario: Quota de tokens esgotada
- **WHEN** o ambiente atinge o teto de tokens do periodo
- **THEN** novas inferencias sao recusadas com motivo de quota e a recusa entra na observabilidade daquele ambiente

### Requirement: Rate limit na API de modelo
O sistema MUST aplicar limite de chamadas a API de modelo por ambiente, mais restrito em producao do que em desenvolvimento.

#### Scenario: Estouro de chamadas
- **WHEN** o numero de chamadas no intervalo passa do limite do ambiente
- **THEN** o sistema recusa ou adia a chamada e registra o evento de rate limit
