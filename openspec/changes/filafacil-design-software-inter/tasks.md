# Tasks

## 1. Organizacao e contratos

- [x] 1.1 Criar pacote feature-based `com.mundodev.filafacil.coletas` e mover/adaptar model + persistencia de coletas para contratos `ColetaRepository`; verificar projeto compila e o terminal ainda lista/cadastra
- [x] 1.2 Extrair interfaces de `PriorizacaoStrategy` e `ColetaObserver` (+ publisher) e injetar no `ColetaService` sem `new` de dependencias no service; verificar wiring no `Main` e teste de instanciação com fakes

## 2. Factory, Strategy e Observer

- [x] 2.1 Implementar `ColetaFactory` (validacao + status inicial `pendente`) e fazer o cadastro usar a factory; verificar teste de criacao valida/invalida
- [x] 2.2 Implementar pelo menos duas strategies (`PrioridadeManualStrategy`, `PrioridadePorPacotesStrategy`) e plugar no service; verificar teste de troca de estrategia sem alterar o service
- [x] 2.3 Registrar observadores concretos (log operacional + contador em memoria) em criacao e atualizacao de status; verificar teste com observer fake/contador

## 3. Paridade funcional, testes e README

- [x] 3.1 Adicionar exclusao de coleta (service + opcao de menu) sem remover fluxos existentes; verificar delete persiste e some da listagem
- [x] 3.2 Reescrever/expandir testes unitarios do dominio coletas com fakes (sem arquivo/banco real) e garantir `mvn test` verde na suite completa
- [x] 3.3 Atualizar README com organizacao feature-based, contratos e onde Factory/Strategy/Observer/DI foram aplicados; verificar nomes/caminhos batem com o codigo

## Workflow follow-up

- Archive a change (`/opsx-archive`) apos review.
- Opcional: archive de changes concluidas anteriores ainda nao arquivadas.
