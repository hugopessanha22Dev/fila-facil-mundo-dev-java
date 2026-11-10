# Spec Delta

## Purpose

Separar configuracao local e de producao por variaveis de ambiente, sem credenciais fixas no codigo-fonte do backend.

## ADDED Requirements

### Requirement: Configuracao externa obrigatoria
A aplicacao MUST obter porta, URL/credenciais de banco, bucket de storage, regiao cloud e modo de execucao (local ou producao) de variaveis de ambiente ou mecanismo equivalente externo ao codigo.

#### Scenario: Inicializacao com env vars
- **WHEN** a API inicia com as variaveis de ambiente obrigatorias definidas
- **THEN** ela usa esses valores para porta, banco, storage, regiao e modo

### Requirement: Zero credenciais no codigo
O codigo-fonte e o repositorio MUST NOT conter senhas, tokens ou chaves de acesso hardcoded.

#### Scenario: Revisao de segredos
- **WHEN** um desenvolvedor inspeciona o codigo versionado
- **THEN** nao encontra credenciais de banco, cloud ou IAM embutidas

### Requirement: Modos local e producao distintos
A aplicacao MUST distinguir claramente modo local e modo producao via configuracao externa.

#### Scenario: Troca de modo
- **WHEN** o modo de execucao e alterado via variavel de ambiente e a API reinicia
- **THEN** o comportamento de conexao (banco/storage) segue o modo configurado
