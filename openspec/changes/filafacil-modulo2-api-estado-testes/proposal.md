# Proposal

## Why

O FilaFácil do Módulo 1 (terminal + JSON) já cobre CRUD, filtros e resumo, mas ainda sem integração externa, estado centralizado, erros tipados nem testes automatizados. O Módulo 2 (intermediário) pede evoluir essa base: refatorar camadas, enriquecer o cadastro via API REST assíncrona, centralizar estado e validar regras com testes — sem web, banco ou frameworks pesados.

## What Changes

- Refinar camadas já existentes (controller / service / store) e isolar integração REST em módulo próprio
- No cadastro, consultar API pública de CEP (ViaCEP) de forma assíncrona e enriquecer a coleta com dados do endereço (bairro, cidade, UF); não concluir o cadastro antes da resposta ou falha tratada
- Introduzir estado centralizado da aplicação: lista de coletas, status de carregamento, último erro e filtros ativos
- Tratar erros por origem (entrada, arquivo, API) com mensagens claras, sem encerrar a aplicação
- Adicionar testes unitários (JUnit) para validação, filtros, resumo e estado — sem testar o menu de terminal
- Atualizar README com API usada, como executar e como rodar testes
- Fluxo Git do módulo: branch dedicada, commits descritivos e PR (executado no apply/entrega)

## Capabilities

### New Capabilities

- `rest-enrichment`: consulta assíncrona a API REST no cadastro e enriquecimento dos dados da coleta (ex.: CEP → bairro/cidade/UF), com falha tratada
- `application-state`: estado central com lista de coletas, loading, último erro e filtros ativos
- `typed-error-handling`: classificação e exposição de erros de entrada, arquivo e API sem crash

### Modified Capabilities

- (nenhuma main spec arquivada em `openspec/specs/`; comportamento do Módulo 1 permanece na base de código e é estendido pelas capabilities acima)

## Impact

- Afeta `Coleta` (novos campos de enriquecimento), `ColetaService`, `MenuController`, `ColetaFileStore`, `pom.xml` (JUnit; HttpClient do JDK 21)
- Novo módulo de cliente HTTP/API e módulo de estado
- Continua terminal + JSON local; sem MySQL, sem API própria, sem auth
- Referência: PDF *Desenvolvimento de Software Intermediário – FilaFácil Logística* (Módulo 2)
