# Tasks

## 1. Domínio

- [x] 1.1 Entidade e value objects: teste de abertura completa, recusa sem código da entrega ou sem descrição, e identificador distinto entre duas exceções
- [x] 1.2 Transições: teste de análise a partir de aberta, resolução recusada enquanto aberta, escala obrigatória para severidade crítica, cancelamento a partir de aberta e evento com status anterior e novo
- [x] 1.3 Teste de que os arquivos do domínio não importam Jackson, Javalin, `java.nio.file` nem JDBC

## 2. Casos de uso e adaptadores

- [x] 2.1 Casos de uso sobre portas: teste de consulta depois da abertura, segunda abertura recusada para a mesma entrega e o mesmo tipo, e resolução recusada depois do fim da janela
- [x] 2.2 Repositório e publicador em memória: teste de que a análise aceita fica consultável e o evento correspondente entra no publicador
- [x] 2.3 Gateway: teste de `route_blocked` virando rota bloqueada com origem roteirização, e de termo desconhecido sem abrir exceção
- [x] 2.4 CLI `exec:java@excecoes` no `pom.xml`, com abrir, analisar, escalar, resolver, cancelar e consultar; recusa de resolução precoce devolve código tratado e não estoura o processo

## 3. Mapa, decisão e menu

- [x] 3.1 `docs/excecoes/context-map.md` com os cinco contextos, gestão como núcleo e auditoria como downstream
- [x] 3.2 `docs/excecoes/adr-0001-orquestrador.md` com as duas comparações, a opção adotada e a evolução da transição da CLI para a entidade, mantendo os seis casos de uso
- [x] 3.3 README com o comando, os status e a observação de que o estado é em memória; o menu continua na opção 12 e sem opção 13
- [x] 3.4 `mvn test` verde, sem arquivo novo em `inference/golden` e sem gravar `data/coletas.json`

## Workflow follow-up

- Smoke manual da CLI com uma exceção crítica fica fora do `mvn test`
- Arquivar o change depois da revisão
