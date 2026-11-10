# Spec Delta

## Purpose

Garantir testabilidade real do service de coletas com fakes/mocks, validando DI, troca de Strategy e notificacao de Observer sem banco ou arquivo reais.

## ADDED Requirements

### Requirement: Testes unitarios do service sem I/O real
O projeto MUST incluir testes unitarios do service de coletas que usam fakes dos contratos e nao dependem de banco nem arquivo real.

#### Scenario: Cadastro com repository fake
- **WHEN** o teste cadastra uma coleta com repository e dependencias fake
- **THEN** a operacao conclui e o fake registra a persistencia esperada

### Requirement: Teste de troca de Strategy
Os testes MUST validar que trocar a estrategia de priorizacao altera o resultado sem modificar o service.

#### Scenario: Duas estrategias no mesmo service
- **WHEN** o mesmo service e exercitado com estrategia A e depois com estrategia B
- **THEN** as prioridades resultantes diferem conforme cada estrategia

### Requirement: Teste de Observer
Os testes MUST verificar que observadores sao notificados nos eventos relevantes.

#### Scenario: Fake observer recebe evento
- **WHEN** ocorre criacao ou atualizacao de status sob teste
- **THEN** o observer fake registra a chamada correspondente

### Requirement: Documentacao de design no README
O README MUST descrever a organizacao por dominio, os contratos e onde Factory, Strategy, Observer e DI foram aplicados.

#### Scenario: Leitura do README
- **WHEN** um avaliador consulta o README apos a refatoracao
- **THEN** encontra a descricao da estrutura feature-based e dos patterns aplicados
