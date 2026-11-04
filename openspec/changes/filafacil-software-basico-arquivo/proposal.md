# Proposal

## Why

O case Mundo Dev de **Desenvolvimento de Software Básico** exige um FilaFácil 100% terminal, sem banco e sem frameworks, com persistência em arquivo local e resumo operacional. O código atual (Hibernate + MySQL) atende o módulo de banco, mas viola as restrições deste case — é preciso realinhar o projeto ao PDF de software básico.

## What Changes

- **BREAKING:** Remover dependência de MySQL, Hibernate/ORM e `schema.sql` voltado a banco
- Reescrever persistência para arquivo local simples (`.json`), carregado na abertura e gravado após cadastro/alteração
- Manter app Java de terminal com menu em loop (cadastrar, listar, filtrar, atualizar, resumir, sair)
- Ajustar domínio aos valores do case: prioridade `baixa`/`media`/`alta`; status `pendente`/`em_rota`/`concluida`; status inicial sempre `pendente`
- Adicionar resumo operacional no terminal (totais de coletas e pacotes; contagens por prioridade e status)
- Organizar lógica em funções/métodos coesos; usar stream `map`/`filter`/`reduce` (ou equivalentes) em filtros e totais
- Atualizar README para execução local sem banco
- Tratar primeira execução sem arquivo existente sem encerrar o programa

## Capabilities

### New Capabilities

- `coleta-management`: cadastro com validação, listagem com identificador, filtros por status/prioridade e atualização de status (domínio do case software básico)
- `operational-summary`: resumo operacional com totais de coletas/pacotes e quantidades por prioridade e status
- `local-file-store`: leitura/escrita em arquivo local e boot seguro quando o arquivo ainda não existe
- `terminal-cli`: menu interativo em loop expondo as operações do case, sem web/API/auth

### Modified Capabilities

- (nenhuma — `openspec/specs/` ainda sem main specs arquivadas; a change anterior de MySQL permanece separada e não sincronizada)

## Impact

- Substitui camadas `HibernateUtil` / repository JPA / `hibernate.cfg.xml` / driver MySQL
- Afeta `pom.xml` (remover deps ORM/MySQL), `ColetaService`, `ColetaRepository`, `MenuController`, `Main`, `README.md`
- Dados passam a viver em arquivo no disco do projeto (ex.: `data/coletas.json`), não no database `fila-facil`
- Referência: PDF *Desenvolvimento de Software Básico – FilaFácil Logística* (Escola Mundo Dev)
