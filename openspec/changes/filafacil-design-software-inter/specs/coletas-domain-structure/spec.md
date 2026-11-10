# Spec Delta

## Purpose

Organizar o dominio de coletas de forma feature-based, com contratos e injecao de dependencias no service, sem acoplar regras a implementacoes concretas.

## ADDED Requirements

### Requirement: Organizacao por dominio de coletas
O codigo do dominio de coletas MUST ficar agrupado por feature (controller/service/repository/contratos/estrategias/fabrica/observadores e testes relacionados), evitando depender apenas de pastas globais por tipo tecnico para esse dominio.

#### Scenario: Localizacao do dominio
- **WHEN** um desenvolvedor procura os artefatos de coletas
- **THEN** encontra controller, service, repository, contratos e patterns do dominio sob a estrutura feature-based documentada

### Requirement: Service depende de contratos
O service de coletas MUST depender de interfaces/contratos para persistencia, estrategia de priorizacao e notificacao, nao de classes concretas fixas.

#### Scenario: Substituicao em teste
- **WHEN** o service e construido com fakes dos contratos
- **THEN** ele executa as operacoes sem acessar arquivo ou banco real

### Requirement: Dependency Injection sem new no service
O service de coletas MUST receber repository, estrategia, fabrica e notificadores por injecao e MUST NOT instanciar essas dependencias com `new` no seu corpo.

#### Scenario: Wiring externo
- **WHEN** a aplicacao inicia o fluxo de coletas
- **THEN** as dependencias do service sao fornecidas pelo composition root (Main/wiring), nao criadas dentro do service

### Requirement: Operacoes de coletas preservadas
O sistema MUST continuar permitindo cadastrar, listar, filtrar, atualizar status, gerar resumo operacional e deletar coletas apos a refatoracao.

#### Scenario: Fluxo basico intacto
- **WHEN** o usuario executa cadastro, listagem, filtro, atualizacao de status, resumo e exclusao
- **THEN** cada operacao permanece disponivel e funcional no terminal
