# Design

## Context

Codigo atual: Maven Java 17, terminal com triagem HTTP OpenAI-compatible (Groq no `.env`, mock nos testes), API de docas em Javalin, Postgres/Redis/Mongo no Compose, IAM e auto scaling da API de docas em `infra/`. Nao ha fila de triagem, banco vetorial, ambientes de IA nem layout de artefatos de inferencia. Ver `proposal.md` — Why. Requisitos observaveis: specs deste change.

Restricao de prazo: cerca de 6 horas, um aluno, um provedor. AWS ja e o provedor do cloud backend. A demonstracao local tem de rodar sem conta paga.

## Goals / Non-Goals

**Goals:**

- Entrypoint novo do Assistente Cloud de Triagem, sem alterar o menu 0–12 nem a API de docas
- O mesmo fluxo nos tres ambientes, com quotas e chaves distintas
- Vetor, fila, cache, storage e serverless com implementacao local executavel e contrato de infra AWS ao lado
- Testes sem rede; ensaio com a Groq opcional

**Non-Goals:**

- Treino, fine-tuning, dado real de cliente, segundo provedor
- Provisionar a conta AWS dentro do `mvn test`
- Reescrever o gateway de triagem do terminal ou corrigir o contrato antigo de `justificativaIntervencencao`
- Substituir Redis de auditoria ou o bucket de anexos de docas

## Decisions

### 1. Pacote e entrypoint proprios

- Pacote `com.mundodev.filafacil.cloudai` com `CloudAiMain`
- CLI: `mvn -q exec:java@cloud-ai` com argumentos `enqueue` e `drain` (publicar uma mensagem simulada e consumir a fila)
- **Por que:** o case e uma plataforma ao lado da triagem local. Um main separado evita misturar o Scanner do menu com fila e storage
- **Alternativa:** opcao 13 no menu — rejeitada para nao acoplar o ensaio cloud ao loop interativo
- **Alternativa:** estender `ApiMain` de docas — rejeitada; docas e triagem de IA tem ciclo de vida e IAM diferentes

### 2. Fila local com o mesmo contrato de uma fila gerenciada

- Interface de fila com publicar, receber e concluir/falhar
- Local: arquivo JSON por ambiente em `data/cloud-ai/{env}/queue/`
- Cloud documentada: SQS, uma fila por ambiente
- **Por que:** o case pede rastreio assincrono; arquivo da para testar accept/process/fail sem LocalStack
- **Alternativa:** SQS real no teste — rejeitada pelo custo e pela rede

### 3. Funcoes serverless como classes puras

- Tres operacoes: validar payload, limpar prompt antes de persistir, normalizar a resposta
- A aplicacao principal so orquestra; ela chama essas operacoes por uma interface, nao embute a regra
- Infra: uma Lambda com discriminador de acao, mesmo papel IAM minimo
- **Por que:** separa compute continuo de tarefa curta sem obrigar deploy de Lambda para o teste
- **Alternativa:** framework serverless no repo — rejeitada no prazo

### 4. API de modelo reusada, com teto da plataforma

- O pipeline chama o `InferenceGateway` ja existente
- Perfil e modelo continuam os do ambiente (`FILAFACIL_LLM_*`), mas o cloud AI aplica timeout, max tokens e rate limit do arquivo de quota daquele ambiente antes da chamada
- Testes injetam gateway fake
- **Por que:** ja existe cliente Groq compativel com OpenAI, retry e custo estimado
- **Alternativa:** cliente novo — rejeitada, duplicaria timeout e chave

### 5. Vetor local versionado, pgvector so na cloud

- Documentos ficticios no classpath, quatro familias
- Embedding local deterministico (vetor esparso por token, cosseno). Sem chamada de embedding na suite
- Indice em `data/cloud-ai/{env}/embeddings/{dominio}/v{versao}/`
- Versao ativa configuravel; versao anterior permanece
- Cloud: DDL pgvector comentado no Postgres que o projeto ja usa, mesma interface
- **Por que:** Groq em cota gratis nao e um servico de embedding estavel para este prazo, e o case proibe chamada ilimitada. A busca e o versionamento sao o comportamento cobrado
- **Alternativa:** OpenSearch ou Pinecone — rejeitada (segundo produto, custo, multiplo servico gerenciado fora das 6 horas)
- Corte de similaridade fixo e baixo o bastante para os documentos de fixture; ausencia de hit devolve contexto vazio

### 6. Cache com chave que inclui versao

- Chave: ambiente + versao de prompt + versao de documentos + texto normalizado
- Valor com prazo. Redis se `FILAFACIL_REDIS_HOST` responder; senao mapa em memoria com o mesmo TTL
- **Por que:** o Compose ja tem Redis. Memoria cobre o teste
- **Alternativa:** cache sem versao na chave — rejeitada porque devolveria resposta velha depois de trocar prompt ou documento

### 7. Secrets e ambientes

- `FILAFACIL_AI_ENV` = `dev` | `homolog` | `prod`
- Quotas, rate limit, timeout e limiar de alerta em recurso versionado (`cloud-ai-quotas.json`), sem segredos
- Chave do modelo: variavel de ambiente injetada, uma por ambiente. Producao e homologacao nao leem a chave de dev
- Arquivo `.env` segue ignorado. Modo prod recusa subir se a chave estiver vazia ou se o processo apontar para um arquivo de secret dentro do repo
- Infra: Secrets Manager por ambiente, com IAM `GetSecretValue` so no ARN daquele ambiente; rotacao fica no cofre, o processo recebe o valor novo na proxima subida
- **Por que:** e o mesmo padrao do cloud backend (plataforma injeta o segredo; o codigo nao busca arquivo commitado)
- **Alternativa:** SDK de Secrets Manager em todo teste — rejeitada; o comportamento observavel e "nao ha chave no repo e cada ambiente tem a sua"

### 8. Layout de artefatos

Prefixos iguais no disco local e no desenho S3:

- `datasets/raw` e `datasets/processed`
- `embeddings/{dominio}/v{versao}`
- `prompts/{id}/v{n}`
- `evals/{modelo}/{data}/{workload}`
- `inferences/{data}/{id}.json`
- `logs/{data}`

Antes de gravar, mascarar padroes de chave (`gsk_`, `sk-`, `AQ.`). O JSON de inferencia guarda id, modelo, versao de prompt, latencia, tokens, custo estimado, status e erro.

### 9. Infra AWS ao lado do codigo

Novos arquivos em `infra/ai/`, sem aplicar na conta durante o change:

- fila por ambiente, Lambda, ALB na frente do compute continuo, auto scaling min/max
- politica IAM sem `AdministratorAccess`: secret do ambiente, prefixos S3 deste dominio, logs, fila
- tags obrigatorias `Environment`, `Owner`, `Purpose` num validador local que falha se faltar
- observabilidade: o drain imprime o resumo (tokens, chamadas, latencia, erro, bytes de storage, profundidade da fila, custo) e grava alerta quando o limiar do ambiente estoura

Compute continuo sugerido: o mesmo estilo App Runner ja documentado para a API de docas, servico distinto.

## Risks / Trade-offs

- [Embedding local nao e um modelo gerenciado] → a interface permite trocar por pgvector/API depois; o teste prova versao, familia e "nao inventar" quando nao ha hit
- [Fila em arquivo nao e SQS] → o contrato e o estado da mensagem sao os mesmos; a infra nomeia a fila real
- [Lambda nao executa na suite] → as tres funcoes sao testadas como unidade; o JSON de infra mostra o encaixe
- [Grafico de pesos do PDF nao saiu legivel] → API de modelo e banco vetorial ficam com mais cenarios de teste, como o texto do case pede
- [Rate limit em memoria nao vale para varias instancias] → suficiente para o criterio do case e para o alerta; producao documenta o limite na borda

## Migration Plan

1. Adicionar pacote, fixtures, quotas e `infra/ai/` sem mexer em menu, docas ou pipelines
2. `mvn test` novo deve passar com gateway fake
3. Rollback: remover o entrypoint e o pacote; `data/cloud-ai/` e local e pode ser apagado. Nada migra coleta nem agendamento

## Open Questions

Nenhuma que mude spec, abordagem ou tarefas. O identificador exato do modelo de cada ambiente reaproveita `FILAFACIL_LLM_MODEL_LIGHT` e `FILAFACIL_LLM_MODEL_DETAILED` ja configurados.
