# Tasks

## 1. Camadas, estado e erros tipados

- [x] 1.1 Criar hierarquia de excecoes (`EntradaException`, `ArquivoException`, `ApiException`) e `AppState` (lista, loading, ultimoErro, filtros); verificar compilacao e estado inicial apos carga
- [x] 1.2 Refatorar `ColetaService`/`ColetaFileStore`/`MenuController` para atualizar e consumir `AppState`, mapeando falhas de arquivo/entrada para os tipos corretos; verificar mensagens sem crash em entrada invalida

## 2. Integracao REST assincrona (ViaCEP)

- [x] 2.1 Implementar `CepApiClient` com `HttpClient` + `CompletableFuture` consultando ViaCEP; verificar resposta JSON mapeada para `EnderecoCep` (sucesso) e `ApiException` (CEP invalido/rede)
- [x] 2.2 Estender `Coleta` com `cep`, `bairro`, `cidade`, `uf` e alterar cadastro para exigir CEP, aguardar API e so persistir apos enriquecimento; verificar cadastro com CEP valido grava campos enriquecidos
- [x] 2.3 Tratar falha de API no fluxo de cadastro (nao persistir, registrar erro no estado, voltar ao menu); verificar que falha nao encerra a aplicacao

## 3. Testes e documentacao

- [x] 3.1 Adicionar JUnit 5 no `pom.xml` e testes unitarios para validacao, filtros, resumo e `AppState` (com fake de `CepApiClient` no cadastro); verificar `mvn test` verde
- [x] 3.2 Atualizar `README.md` com API ViaCEP, novos campos, como executar e como rodar testes; verificar instrucoes alinhadas ao codigo
- [x] 3.3 Smoke manual: cadastro com CEP real, listagem mostrando enriquecimento, filtro, resumo, e tentativa com CEP invalido; confirmar specs REST/estado/erros

## Workflow follow-up

- Criar branch do modulo 2, commits descritivos e abrir PR quando houver remoto.
- Arquivar esta change apos apply/revisao; considerar arquivar changes antigas ainda abertas.
