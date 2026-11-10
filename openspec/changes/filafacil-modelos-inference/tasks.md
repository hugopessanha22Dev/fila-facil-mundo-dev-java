# Tasks

## 1. Foundation do pacote inference

- [x] 1.1 Criar pacote `com.mundodev.filafacil.inference` (gateway, routing, schema, tools, eval) e `InferenceException` tipada; verificar compile com `mvn -q compile`
- [x] 1.2 Adicionar `src/main/resources/inference/triage-schema.json` com campos obrigatórios da spec `structured-triage`; verificar arquivo presente no classpath de teste/recursos
- [x] 1.3 Documentar no README as env vars (`FILAFACIL_LLM_*`) e defaults OpenAI-compatible; verificar seção legível e sem chaves reais

## 2. Gateway e validação

- [x] 2.1 Implementar `MockInferenceGateway` (sem rede) retornando JSON de triagem controlado; verificar teste unitário passa com `FILAFACIL_LLM_MODE=mock`
- [x] 2.2 Implementar cliente HTTP OpenAI-compatible com timeout, retries limitados, auth por env e log de latência/custo estimado; verificar falha tipada quando chave ausente em modo http
- [x] 2.3 Implementar fallback primário→secundário no gateway; verificar teste mock força falha no primário e sucesso no fallback
- [x] 2.4 Implementar `TriageSchemaValidator` rejeitando JSON inválido/incompleto; verificar testes de aceitar válido e rejeitar inválido

## 3. Roteamento e tools

- [x] 3.1 Implementar `ModelRouter` (leve vs detalhado) com heurísticas de criticidade/tamanho e parâmetros por cenário; verificar testes unitários cobrem caso simples → leve e reclamação crítica → detalhado
- [x] 3.2 Definir interfaces das 5 tools com contratos entrada/saída/erro e mocks (busca coleta via estado/lista quando possível); verificar testes de sucesso e id inexistente
- [x] 3.3 Integrar tools mockáveis ao fluxo de inferência (ou validação unitária equivalente se o provedor não emitir tool calls); verificar pelo menos uma tool exercitada nos testes

## 4. Menu e wiring

- [x] 4.1 Ligar gateway/router/validator em `Main` conforme `FILAFACIL_LLM_MODE`; verificar app inicia em mock sem chave
- [x] 4.2 Adicionar opção de menu “Triagem IA” em `MenuController` (texto → resultado estruturado ou erro tipado no `AppState`); verificar fluxo manual em mock imprime JSON válido e CRUD existente permanece

## 5. Avaliação offline

- [x] 5.1 Criar golden dataset (`src/test/resources/inference/golden/`) com exemplos de consulta, ocorrência e reclamação + expected parcial; verificar arquivos carregáveis
- [x] 5.2 Implementar runner/teste de eval offline (JSON válido, campos, latência, custo, fallback) em modo mock; verificar `mvn test` reporta métricas ou asserções equivalentes
- [x] 5.3 Documentar no README comparação dos 2 perfis, como rodar eval e análise de falhas exemplo; verificar instruções batem com comandos reais

## Workflow follow-up

- Archive a change após review (`/opsx-archive`) quando a implementação estiver estável.
- Confirmar provedor/modelIds finais com o usuário se os defaults OpenAI-compatible precisarem mudar.
