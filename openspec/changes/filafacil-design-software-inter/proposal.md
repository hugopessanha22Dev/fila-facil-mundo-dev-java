# Proposal

## Why

O FilaFácil de coletas ja funciona (terminal + JSON + ViaCEP + resumo), mas o dominio esta organizado por camadas tecnicas (`service`/`store`/`model`) e o `ColetaService` concentra validacao, montagem de entidade, persistencia e regras sem contratos de Factory/Strategy/Observer. O case Mundo Dev **Design de Software Intermediario** pede exatamente essa refatoracao de design interno (~6h), sem expandir o negocio.

## What Changes

- Reorganizar o dominio de coletas em estrutura feature-based (controller/service/repository/contratos/strategies/factory/observers/testes coesos)
- Introduzir contratos (`ColetaRepository`, estrategia de priorizacao, notificador/observer) e Dependency Injection completa no service (zero `new` de dependencias dentro do service)
- Aplicar **Factory** para criar coletas validas (campos obrigatorios, status inicial, data/prioridade)
- Aplicar **Strategy** com pelo menos duas estrategias de priorizacao intercambiaveis sem alterar o service
- Aplicar **Observer** para eventos de criacao e atualizacao de status (ex.: log operacional, contador em memoria, notificador de rota)
- Manter funcionalidades: cadastrar, listar, filtrar, atualizar status, resumo operacional; incluir **deletar** (hoje ausente) como paridade do case, sem novas features grandes
- Testes unitarios do service com fakes (sem banco/arquivo real); documentar organizacao e patterns no README
- Nao tocar no escopo de inference/manifest/API cloud alem do necessario para compilar apos o move de pacotes

## Capabilities

### New Capabilities

- `coletas-domain-structure`: organizacao feature-based do dominio de coletas com contratos e DI
- `coleta-factory`: criacao centralizada de coletas validas via Factory
- `prioritization-strategy`: pelo menos duas estrategias de priorizacao trocaveis
- `coleta-event-observers`: observadores notificados em criacao e mudanca de status
- `coleta-service-testability`: testes unitarios do service com fakes/mocks e troca de estrategia

### Modified Capabilities

- (nenhuma main spec arquivada; comportamento externo de coletas permanece e e reforçado com delete)

## Impact

- Pacotes atuais `service`/`store`/`model`/`controller` relacionados a coletas migrados/agrupados sob dominio (ex.: `com.mundodev.filafacil.coletas.*`)
- `Main` / `MenuController` atualizados para wiring por DI
- Novos testes em `src/test/java/.../coletas/`
- README: secao de design (organizacao, contratos, onde Factory/Strategy/Observer/DI foram aplicados)
- Inference, manifesto e API de agendamentos permanecem; evitar over-engineering e mensageria real
- Referencia: PDF *Design de Software Inter – FilaFácil Logística* (Escola Mundo Dev)
