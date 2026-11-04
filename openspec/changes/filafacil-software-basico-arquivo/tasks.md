# Tasks

## 1. Remover stack MySQL/ORM e ajustar build

- [x] 1.1 Remover do `pom.xml` dependencias Hibernate, MySQL Connector e configs ORM; verificar `mvn -q validate` com sucesso
- [x] 1.2 Remover `HibernateUtil`, `hibernate.cfg.xml`, `schema.sql` e codigo JPA do model/repository; verificar que o projeto compila sem referencias a Hibernate/JPA (`mvn -q compile`)

## 2. Modelo, arquivo local e servico core

- [x] 2.1 Recriar `Coleta` como POJO (id, nome, endereco, pacotes, prioridade, status) com valores do PDF; verificar instanciação e `toString` uteis no terminal
- [x] 2.2 Implementar `ColetaFileStore` (carregar/salvar `data/coletas.json`) tratando arquivo ausente como lista vazia; verificar boot sem arquivo e gravacao apos salvar
- [x] 2.3 Implementar `ColetaService.cadastrar` com validacao de obrigatorios, status inicial `pendente`, id incremental e persistencia no arquivo; verificar rejeicao de vazios e persistencia real
- [x] 2.4 Implementar listagem e filtros por status/prioridade com Streams `filter`; verificar listagem completa e filtros com/sem resultados

## 3. Atualizacao, resumo e menu

- [x] 3.1 Implementar atualizacao de status por id (`pendente`/`em_rota`/`concluida`) com checagem de existencia e regravacao do arquivo; verificar sucesso e id inexistente
- [x] 3.2 Implementar resumo operacional (totais de coletas/pacotes + contagens por prioridade e status) usando `map`/`filter`/`reduce` (ou Collectors); verificar saida no terminal com dados e sem dados
- [x] 3.3 Reescrever `MenuController` + `Main` com opcoes cadastrar/listar/filtrar/atualizar/resumo/sair, opcao invalida sem encerrar; verificar loop e feedbacks
- [x] 3.4 Atualizar `README.md` para execucao sem banco (JDK, Maven, `mvn exec:java`, arquivo `data/coletas.json`); verificar que as instrucoes batem com o codigo

## 4. Verificacao ponta a ponta

- [x] 4.1 Rodar smoke no terminal: cadastro, listagem, 2 filtros, atualizacao, resumo, reinicio carregando JSON; confirmar alinhamento as specs desta change

## Workflow follow-up

- Arquivar `filafacil-software-basico-arquivo` apos apply/revisao.
- Decidir o destino da change MySQL ainda aberta (`filafacil-coletas-java-mysql`): arquivar com/sem sync ou manter como historico separado.
