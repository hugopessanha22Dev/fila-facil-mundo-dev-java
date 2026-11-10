# Design

## Context

Codigo atual: `ColetaService` em `service/` com DI parcial (`ColetaFileStore`, `AppState`, `CepApiClient`), montagem manual de `Coleta`, prioridade como string do usuario, sem Observer, pacotes por camada tecnica. Delete de coleta ausente. Ver `proposal.md` — Why.

Restricoes do case: ~6h, sem web, sem microsservicos, sem mensageria real, sem over-engineering, nao remover funcionalidades.

## Goals / Non-Goals

**Goals:**

- Pacote feature-based `com.mundodev.filafacil.coletas.*`
- Contratos + DI total no service de coletas
- Factory, Strategy (>=2), Observer (>=2 concretos)
- Testes unitarios com fakes; README de design
- Preservar menu/CRUD/resumo; adicionar delete; nao quebrar inference/manifest/api

**Non-Goals:**

- Novas features grandes de negocio, UI web, fila/mensageria
- Refatorar inference/api cloud para o mesmo padrao neste change
- Framework DI (Spring); wiring manual no `Main` basta

## Decisions

### 1. Estrutura feature-based

```
com.mundodev.filafacil.coletas
  Coleta.java (model)
  ColetaService.java
  ColetaController ou uso via MenuController adaptado
  repo/
    ColetaRepository.java
    FileColetaRepository.java  (adapta ColetaFileStore)
  factory/
    ColetaFactory.java
  priority/
    PriorizacaoStrategy.java
    PrioridadeManualStrategy.java
    PrioridadePorPacotesStrategy.java
  observer/
    ColetaEvent.java / ColetaObserver.java
    ColetaEventPublisher.java
    LogOperacionalObserver.java
    ContadorMemoriaObserver.java
    NotificacaoRotaObserver.java (opcional terceiro, leve)
```

- `MenuController` pode permanecer em `controller/` chamando `ColetaService` movido, **ou** ganhar facade em `coletas` — preferencia: mover service/store/model de coleta para `coletas` e manter `MenuController` no composition UI para nao espalhar menu.
- **Por que:** atende "evitar pastas globais por tipo tecnico" no dominio de coletas sem forcar move de inference/api.
- **Alternativa:** monorepo hexagonal completo — rejeitada (over-engineering / >6h).

### 2. Contratos e DI

- `ColetaRepository`: saveAll/load/find/delete (ou API minima alinhada ao store atual)
- `PriorizacaoStrategy`: `String decidir(DadosPriorizacao ctx)`
- `ColetaObserver` + publisher injetado
- `ColetaFactory` injetada
- `CepApiClient` continua contrato existente injetado
- `AppState` pode permanecer; repository encapsula persistencia arquivo
- Composition root: `Main` (e testes) fazem `new` das concretas e injetam no service
- Regra: **nenhum** `new Repository/Strategy/Factory/Observer/CepClient` dentro de `ColetaService`

### 3. Factory

- Entrada: nome, endereco, pacotes, dados de CEP enriquecidos, sinal de prioridade (manual ou contexto para strategy)
- Saida: `Coleta` com id (fornecido pelo service/repo), status `pendente`, prioridade ja resolvida pela strategy chamada pelo service **antes ou dentro** do fluxo de factory
- Decisao: service chama `strategy.decidir(...)` e passa prioridade para `factory.criar(...)`, mantendo Factory focada em validar/montar entidade (SRP)

### 4. Strategy (minimo 2)

1. `PrioridadeManualStrategy` — usa a prioridade informada pelo usuario (comportamento atual)
2. `PrioridadePorPacotesStrategy` — ex.: pacotes >= 10 → alta; >= 5 → media; senao baixa (ignora ou sobrescreve manual conforme regra documentada)

Default no `Main`: manual (compatibilidade UX). Testes cobrem troca para por-pacotes.

### 5. Observer

- Publisher notifica lista de observers em `coletaCriada` e `statusAtualizado`
- Concretos: `LogOperacionalObserver` (stdout/slf4j), `ContadorMemoriaObserver` (contagens para assert)
- Sem broker; chamada sincrona in-process

### 6. Delete

- Adicionar `deletar(id)` no service + opcao de menu (ex.: 9 ou integrar no fluxo existente sem quebrar 1–8)
- Persistencia via repository; emitir evento opcional de exclusao **nao obrigatorio** pelo case (case cita observer em criar/atualizar status)

### 7. Testes

- `ColetaServiceTest` reescrito com fakes de repository/CEP/observers/strategies
- Testes dedicados de Factory e Strategies
- Manter suite inference/manifest/api verde (ajustar imports se packages mudarem)

### 8. README

Secao "Design de Software (coletas)": arvore de pacotes, contratos, tabela Pattern → classe, como trocar Strategy.

## Risks / Trade-offs

- [Move de packages quebra imports] → Mitigation: refactor mecanico + `mvn test`
- [Strategy por pacotes muda UX se virar default] → Mitigation: default manual
- [Abstracao excessiva] → Mitigation: so 3 contratos + factory + publisher
- [Menu lotado] → Mitigation: uma opcao delete clara; nao criar submenu complexo

## Migration Plan

1. Criar pacote `coletas` + contratos; compilar com adapters
2. Extrair Factory/Strategy/Observer; injetar no service; remover `new` internos
3. Add delete + testes fakes + README
4. Rollback: reverter package move; terminal volta ao service monolito

## Open Questions

- Nenhuma bloqueante: default Strategy = manual; delete como opcao de menu dedicada.
