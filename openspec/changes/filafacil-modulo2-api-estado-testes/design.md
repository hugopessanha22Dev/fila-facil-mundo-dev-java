# Design

## Context

Base atual (Módulo 1): Java 21 + Maven, `MenuController` → `ColetaService` → `ColetaFileStore` (`data/coletas.json`), sem API externa, sem estado central, sem testes. Ver `proposal.md` — Why. PDF Módulo 2: refatoração, REST async, estado, erros tipados, testes; continua terminal sem DB/web/auth.

## Goals / Non-Goals

**Goals:**

- Isolar cliente REST e estado; enriquecer cadastro com ViaCEP via `CompletableFuture`
- Erros tipados (`EntradaException`, `ArquivoException`, `ApiException`) refletidos em `AppState`
- JUnit 5 para validação, filtros, resumo e estado
- README com API e `mvn test`

**Non-Goals:**

- Interface web, banco, API própria, filas/workers
- Testes do menu/Scanner
- Frameworks pesados (Spring, etc.)

## Decisions

### 1. API pública: ViaCEP

- URL: `https://viacep.com.br/ws/{cep}/json/`
- No cadastro, pedir **CEP** além de nome/endereço/pacotes/prioridade.
- Campos novos em `Coleta`: `cep`, `bairro`, `cidade`, `uf` (preenchidos após sucesso).
- Se ViaCEP retornar `erro: true` ou HTTP/rede falhar → `ApiException`; cadastro **não** persiste coleta incompleta (alinhado a “não finalizar antes da resposta ou falha tratada”: falha = abortar cadastro com mensagem, sucesso = persistir enriquecido).
- **Alternativa:** mock local HTTP — disponível se rede bloquear; default ViaCEP real.

### 2. Async em Java: `CompletableFuture` + `HttpClient`

- `CepApiClient.buscar(String cep)` retorna `CompletableFuture<EnderecoCep>`.
- `ColetaService.cadastrar(...)` faz `.get()` (ou join) no fluxo do terminal — equivalente a await — sem avançar persistência antes.
- I/O de arquivo permanece síncrono no store; se quiser alinhar ao PDF “async I/O”, envolver `CompletableFuture.supplyAsync` no load/save opcionalmente — **default:** async obrigatório na API; file I/O pode ficar sync (já isolado) para não complicar o terminal.

### 3. Pacotes / camadas

```
com.mundodev.filafacil
  Main
  controller.MenuController
  service.ColetaService
  state.AppState
  integration.CepApiClient
  store.ColetaFileStore
  model.Coleta
  model.EnderecoCep
  error.EntradaException | ArquivoException | ApiException | AppException
```

- `AppState`: `List<Coleta> coletas`, `boolean loading`, `AppError ultimoErro`, `FiltrosAtivos filtros`.
- Service atualiza estado em toda operação; controller lê mensagens de erro do estado/exceções.

### 4. Testes

- JUnit 5 + (opcional) Mockito só se necessário para mockar `CepApiClient`.
- Preferir injetar `CepApiClient` no service para testes com fake.
- Cobrir: validação cadastro, filtros, resumo, mutações de `AppState`, enriquecimento com client fake.

### 5. Git do módulo (apply)

- Branch `feature/modulo-2-api-estado-testes` (ou similar); commits por fase; PR fica a cargo do usuário se remoto existir.

## Risks / Trade-offs

- [ViaCEP fora do ar] → Mitigation: fake/mock no teste; mensagem clara em runtime; documentar fallback no README.
- [`.get()` no terminal bloqueia UI] → Mitigation: aceitável em app CLI single-thread; timeout no HttpClient.
- [Campos novos quebram JSON antigo] → Mitigation: Jackson ignora unknown; campos novos null-safe na leitura.

## Migration Plan

1. Branch módulo 2.
2. Introduzir estado + erros + client CEP; estender `Coleta` e cadastro.
3. Testes unitários; atualizar README.
4. Validar smoke com CEP real (ex.: `01001000`).

## Open Questions

Nenhum bloqueante. Assunção: CEP obrigatório no cadastro do Módulo 2; falha de API aborta aquele cadastro (não grava parcial).
