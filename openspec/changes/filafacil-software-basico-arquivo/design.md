# Design

## Context

Codigo atual: Java 21 + Maven + Hibernate + MySQL (`fila-facil`), camadas controller/service/repository JPA. Ver `proposal.md` — Why. Este design realinha o repo ao PDF de **Desenvolvimento de Software Basico** (arquivo local, sem DB/frameworks).

Observado no codigo: `Coleta` JPA, `ColetaRepository` com Session Hibernate, status `PENDENTE/EM_ANDAMENTO/...`, prioridades uppercase. O PDF usa `pendente/em_rota/concluida` e `baixa/media/alta`.

## Goals / Non-Goals

**Goals:**

- Substituir ORM/MySQL por store em JSON local
- Manter Java puro (JDK + Maven apenas para build/exec), menu terminal e funcoes separadas
- Usar Streams (`filter`/`map`/`reduce`) em filtros e resumo
- Incluir opcao de resumo operacional
- README sem passos de banco

**Non-Goals:**

- Manter compatibilidade com o schema MySQL da change anterior
- Spring, Hibernate, JDBC, web, API, auth
- Multiplos arquivos de dados ou migracao automatica MySQL → JSON

## Decisions

### 1. Remover stack de banco e manter Maven + Java SE

- **Escolha:** eliminar deps Hibernate/MySQL/`schema.sql`/hibernate.cfg; app Java SE com `Main` + Scanner.
- **Por que:** o PDF proibe banco e frameworks; Maven so organiza build (nao e framework de app).
- **Alternativa:** manter MySQL e “fingir” o case — rejeitada pelo usuario (opcao 2).

### 2. Persistencia: `data/coletas.json`

- Lista JSON de objetos `{ id, nome, endereco, pacotes, prioridade, status }`.
- `ColetaFileStore`: `carregar()` → `List<Coleta>`; `salvar(List<Coleta>)`.
- Se arquivo ausente: retornar lista vazia; criar arquivo na primeira gravacao.
- Serializacao: `jackson` leve **ou** `org.json` **ou** JSON manual simples. Preferencia: **Jackson databind** apenas como biblioteca de I/O JSON (nao e framework de aplicacao). Se quiser zero libs extras, JSON manual com `PrintWriter`/`Scanner` tambem e aceitavel — default no apply: Jackson por robustez, documentado no README.
- **Alternativa:** `.txt` linha a linha — menos ergonomico para estrutura da coleta.

### 3. Modelo em memoria e IDs

- `Coleta` como POJO simples (sem JPA).
- Id numerico incremental: `max(id)+1` (ou contador) gerado localmente no cadastro.
- Status inicial fixo: `pendente`.
- Dominio: prioridades `baixa|media|alta`; status `pendente|em_rota|concluida`.

### 4. Camadas / funcoes

```
com.mundodev.filafacil
  Main
  model.Coleta
  store.ColetaFileStore
  service.ColetaService   // cadastrar, listar, filtrar*, atualizar, resumir
  controller.MenuController
```

- Remover `config.HibernateUtil` e `repository.ColetaRepository` (JPA).
- Service mantem lista em memoria sincronizada com o store apos mutacoes.
- Filtros: `stream().filter(...)`.
- Resumo: `stream().map(...).reduce(...)` / `Collectors.counting()` / soma de pacotes via `mapToInt().sum()`.

### 5. Menu

Opcoes: 1 cadastrar, 2 listar, 3 filtrar (submenu status/prioridade), 4 atualizar status, 5 resumo, 0 sair. Validar opcao invalida sem sair.

### 6. Convivencia com change MySQL nao arquivada

- Artefatos OpenSpec da change `filafacil-coletas-java-mysql` permanecem em `openspec/changes/` ate o usuario arquivar.
- Este design sobrescreve o codigo de aplicacao; nao sincroniza specs da change antiga.

## Risks / Trade-offs

- [Perda dos dados MySQL existentes] → Mitigation: nao ha migracao automatica; usuario pode exportar manualmente se precisar; case basico espera arquivo novo.
- [Jackson contado como “framework” por avaliador rigoroso] → Mitigation: se necessario no apply, trocar por JSON manual sem dependencia.
- [Duas changes OpenSpec ativas com specs de nomes parecidos] → Mitigation: capabilities novas sob esta change; arquivar a antiga depois evita conflito em main specs.

## Migration Plan

1. Remover deps/config de banco do `pom.xml` e resources Hibernate.
2. Introduzir POJO + FileStore + Service com streams + Menu com resumo.
3. Atualizar README; apagar `schema.sql` ou mover para `docs/legado/` se quiser preservar referencia (preferencia: remover do root para nao confundir o case).
4. Rollback: restaurar commit anterior com Hibernate (git), se ainda existir historico.

## Open Questions

Nenhum bloqueante. Default: Jackson para JSON; valores de dominio exatamente como no PDF (minusculos com underscore em `em_rota`).
