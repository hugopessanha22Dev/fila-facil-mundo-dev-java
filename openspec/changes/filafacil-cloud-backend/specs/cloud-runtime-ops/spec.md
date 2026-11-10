# Spec Delta

## Purpose

Operar a API publicada na cloud com health check, load balancer, auto scaling documentado, IAM minimo e logs de producao sem dados sensiveis.

## ADDED Requirements

### Requirement: Endpoint de health
A API MUST expor um endpoint `/health` que indique disponibilidade para checagem do load balancer.

#### Scenario: Health ok
- **WHEN** a API esta em execucao e recebe GET `/health`
- **THEN** responde com sucesso indicando que o servico esta disponivel

### Requirement: Publicacao via load balancer
A API MUST ser publicavel atras de um load balancer que encaminhe trafego externo aos endpoints principais.

#### Scenario: Acesso externo
- **WHEN** a API esta publicada com load balancer configurado
- **THEN** clientes externos alcançam os endpoints de agendamento e `/health` pela URL publica

### Requirement: Auto scaling documentado
O deploy MUST incluir auto scaling com limites minimo e maximo documentados e regra simples (CPU ou requisicoes).

#### Scenario: Documentacao de scaling
- **WHEN** um operador consulta o README ou artefato de infra do projeto
- **THEN** encontra min/max de instancias e a metrica/regra de escalonamento

### Requirement: IAM de permissoes minimas
A identidade da aplicacao MUST ter acesso apenas aos recursos necessarios de banco, storage e logs, sem permissoes administrativas amplas.

#### Scenario: Politica minima
- **WHEN** a politica IAM da aplicacao e revisada
- **THEN** ela cobre somente banco, storage e logs necessarios, sem admin amplo

### Requirement: Logs de producao sem dados sensiveis
A plataforma cloud MUST receber logs de inicializacao, chamadas, erros de banco/storage e status de health, sem senhas, tokens ou dados pessoais desnecessarios.

#### Scenario: Erro registrado com seguranca
- **WHEN** ocorre erro de banco ou storage em producao
- **THEN** o log registra o evento e o tipo de falha sem incluir credenciais ou PII sensivel
