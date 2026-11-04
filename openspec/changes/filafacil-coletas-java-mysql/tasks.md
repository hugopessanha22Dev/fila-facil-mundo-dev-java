# Tasks

## 1. Bootstrap Maven, MySQL e ORM

- [x] 1.1 Criar `pom.xml` Maven (Java 21) com Hibernate ORM + driver MySQL e verificar `mvn -q validate` com sucesso
- [x] 1.2 Adicionar `schema.sql` criando database `` `fila-facil` `` e tabela `coletas` conforme design; verificar aplicacao manual no MySQL local (`SHOW TABLES` / `DESCRIBE coletas`)
- [x] 1.3 Implementar `HibernateUtil` + `hibernate.cfg.xml` apontando para `jdbc:mysql://localhost:3306/fila-facil` e verificar que a SessionFactory/EntityManagerFactory sobe sem erro
- [x] 1.4 Criar entidade JPA `Coleta` mapeada para `coletas` (id, nome, endereco, pacotes, prioridade, status, dataCriacao) e verificar metadados Hibernate sem falha de mapeamento

## 2. Repository e cadastro/listagem

- [x] 2.1 Implementar `ColetaRepository` com insert (`persist`) e listagens ordenadas por id e por dataCriacao; verificar persistencia real no MySQL apos reinicio da JVM
- [x] 2.2 Implementar `ColetaService.cadastrar` com rejeicao de campos obrigatorios vazios e `pacotes > 0`; verificar cadastro valido persiste e invalido nao grava linha
- [x] 2.3 Implementar listagem no service delegando ao repository; verificar retorno ordenado conforme criterio pedido

## 3. Filtros, update e delete

- [x] 3.1 Adicionar no repository/service filtros JPQL por status, prioridade e nome (igualdade); verificar cada filtro retorna so registros correspondentes e lista vazia quando nao ha match
- [x] 3.2 Implementar atualizacao de status por id com checagem de existencia; verificar sucesso em id existente e mensagem de nao encontrado sem alterar banco
- [x] 3.3 Implementar exclusao por id com checagem de existencia; verificar remocao efetiva e falha segura para id inexistente

## 4. Menu terminal, camadas e README

- [x] 4.1 Implementar `MenuController` + `Main` com loop (cadastrar, listar, filtrar, atualizar status, deletar, sair) delegando so ao service; verificar que controller/repository nao misturam responsabilidades (controller sem EntityManager)
- [x] 4.2 Exibir feedbacks claros de sucesso, validacao e nao encontrado no terminal; verificar mensagens nas operacoes principais
- [x] 4.3 Escrever `README.md` com pre-requisitos, criacao do banco `fila-facil`, credenciais, comando de execucao e mapa do menu; verificar que as instrucoes batem com os arquivos reais do repo
- [x] 4.4 Rodar smoke manual ponta a ponta no terminal (CRUD + 3 filtros + reinicio) e confirmar comportamento alinhado as specs `coleta-management` e `terminal-cli`

## Workflow follow-up

- Apos implementacao e revisao, arquivar a change com `/opsx-archive` (ou equivalente).
- Push remoto e commits incrementais ficam a criterio do usuario na entrega do case.
