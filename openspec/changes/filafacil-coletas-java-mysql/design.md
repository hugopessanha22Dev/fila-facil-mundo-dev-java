# Design

## Context

Repositorio greenfield (sem codigo de aplicacao). Ver `proposal.md` para motivacao e escopo do case Mundo Dev. Restricoes externas: app estritamente terminal, uma tabela, ORM basico, MySQL local `fila-facil`, sem web/API/auth/joins.

## Goals / Non-Goals

**Goals:**

- Bootstrap Maven Java executavel via terminal com Hibernate (JPA) + MySQL
- Modelo `Coleta` mapeado para tabela unica, repository isolando Session/EntityManager
- Camadas controller → service → repository com menu em loop
- Script/README para criar o banco `fila-facil` e rodar o app

**Non-Goals:**

- Spring Boot / API REST / UI web
- Multiplas tabelas, relacionamentos, migracoes Flyway/Liquibase avancadas
- Autenticacao, roles, testes de integracao obrigatorios neste modulo (unitarios opcionais)
- Deploy remoto ou Docker obrigatorio

## Decisions

### 1. Stack: Java 21 + Maven + Hibernate ORM (JPA) + MySQL Connector

- **Escolha:** Hibernate com anotações JPA e `EntityManager`, projeto Maven `jar` com `main` de terminal.
- **Por que:** atende "ORM basico" do case sem puxar Spring Web; o aluno ve mapeamento, persistencia e queries Criteria/JPQL equivalentes a WHERE.
- **Alternativas:** Spring Boot + Data JPA (mais DI, mais magia); JDBC puro (nao cumpre ORM).

### 2. Banco `fila-facil` e tabela `coletas`

- Criar database com nome exato `fila-facil` (identificador com hifen via backticks no MySQL).
- Tabela unica `coletas`:

| Coluna | Tipo | Notas |
|--------|------|--------|
| `id` | BIGINT PK AUTO_INCREMENT | identificador unico |
| `nome` | VARCHAR(120) NOT NULL | nome do cliente |
| `endereco` | VARCHAR(255) NOT NULL | |
| `pacotes` | INT NOT NULL | > 0 |
| `prioridade` | VARCHAR(20) NOT NULL | valores: `BAIXA`, `MEDIA`, `ALTA` |
| `status` | VARCHAR(20) NOT NULL | valores: `PENDENTE`, `EM_ANDAMENTO`, `CONCLUIDA`, `CANCELADA` |
| `data_criacao` | TIMESTAMP NOT NULL | preenchido no cadastro (UTC ou local do JVM) |

- Schema inicial via `schema.sql` documentado no README; runtime pode usar `hibernate.hbm2ddl.auto=update` para ambiente local de estudo.
- **Alternativas:** ENUM nativo MySQL (menos portavel no mapeamento); UUID como PK (desnecessario neste modulo).

### 3. Camadas e pacotes

```
com.mundodev.filafacil
  Main
  model.Coleta
  repository.ColetaRepository
  service.ColetaService
  controller.MenuController
  config.HibernateUtil
```

- `MenuController`: Scanner + loop do menu; parse de entrada; chama service; imprime resultados.
- `ColetaService`: valida obrigatorios (nao vazios), ranges basicos (`pacotes > 0`), existencia antes de update/delete; nao abre Session.
- `ColetaRepository`: `persist`, `findAllOrderById/Data`, `findByStatus/Prioridade/Nome`, `updateStatus`, `deleteById` via EntityManager; unica camada com API Hibernate.

### 4. Queries / filtros

- Listagem: JPQL `ORDER BY c.id` ou `ORDER BY c.dataCriacao`.
- Filtros: JPQL parametrizado (`WHERE c.status = :status`, etc.); nome com igualdade exata inicialmente (simples e alinhado a WHERE basico). Se quiser busca parcial depois, `LIKE` fica fora do escopo minimo.
- Update/delete: `find` por id; se null, service retorna falha tipada/mensagem; se presente, merge/remove.

### 5. Configuracao de conexao

- Arquivo `src/main/resources/hibernate.cfg.xml` (ou `persistence.xml`) com JDBC URL `jdbc:mysql://localhost:3306/fila-facil`, usuario/senha locais documentados no README (sem commit de segredos reais; placeholder `root`/vazio tipico de lab).
- Credenciais sobrescriviveis por propriedades de sistema ou env se conveniente.

### 6. Entrega Git + README

- README cobrindo: pre-requisitos (JDK 21, MySQL), `CREATE DATABASE \`fila-facil\``, aplicar schema, configurar credenciais, `mvn compile exec:java` (ou equivalente), mapa do menu.
- Commits por fase (banco/ORM → insert/select → filtros/update/delete → camadas/README), alinhados a linha do tempo do PDF.

## Risks / Trade-offs

- [MySQL com hifen no nome do DB] → Mitigation: sempre usar backticks / URL JDBC com database name encoded; documentar no README.
- [hbm2ddl.update esconde erro de schema] → Mitigation: manter `schema.sql` como fonte explicita e validar colunas no README.
- [Valores de status/prioridade nao estavam no PDF] → Mitigation: enums documentados acima; service rejeita valor fora da lista.
- [Filtro de nome exato pode frustrar] → Mitigation: aceito no escopo basico; nao adicionar LIKE ate o case pedir.
- [Sem Spring, wiring manual] → Mitigation: `HibernateUtil` singleton + injecao manual no `Main` — adequado ao modulo de 6h.

## Migration Plan

1. Instalar/criar MySQL local e database `fila-facil`.
2. Aplicar `schema.sql` (ou subir app com ddl update).
3. Configurar credenciais em `hibernate.cfg.xml`.
4. Compilar e executar o menu; validar CRUD + filtros.
5. Rollback: dropar tabela/database; codigo permanece local sem efeito remoto.

## Open Questions

Nenhum bloqueante: valores de status/prioridade e igualdade exata no filtro de nome ficam assumidos neste design e podem ser ajustados sem mudar as specs de capacidade.
