# Spec Delta

## Purpose

Define o estado centralizado da aplicacao FilaFacil no Modulo 2: lista de coletas, status de carregamento, ultimo erro e filtros ativos, atualizado pelos servicos.

## ADDED Requirements

### Requirement: Estado central com lista e metadados
O sistema MUST manter uma estrutura central de estado contendo ao menos: lista de coletas, status de carregamento, ultimo erro e filtros ativos.

#### Scenario: Estado apos carga inicial
- **WHEN** a aplicacao inicia e carrega o arquivo JSON com sucesso
- **THEN** o estado MUST refletir a lista carregada, loading finalizado e ultimo erro vazio

### Requirement: Atualizar estado em operacoes
Operacoes de cadastro, filtragem e atualizacao MUST atualizar o estado central (lista e/ou filtros e/ou erro), em vez de espalhar listas paralelas nao sincronizadas.

#### Scenario: Filtro atualiza filtros ativos
- **WHEN** o usuario aplica um filtro por status ou prioridade
- **THEN** o estado MUST registrar os filtros ativos correspondentes

#### Scenario: Cadastro atualiza lista no estado
- **WHEN** um cadastro e concluido com sucesso
- **THEN** a lista de coletas no estado MUST incluir a nova coleta
