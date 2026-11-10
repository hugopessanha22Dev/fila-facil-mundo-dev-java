# FilaFacil Logistica

Aplicação Java de **terminal** para gerenciar coletas com:

- Persistência local em JSON (`data/coletas.json`)
- Enriquecimento assíncrono via **ViaCEP** no cadastro
- Estado centralizado (`AppState`)
- Erros tipados (entrada / arquivo / API / inference)
- **Triagem operacional com IA** (gateway, roteamento, JSON schema, tools mock, eval offline)
- **Prompt engineering**: biblioteca de prompts, few-shot, atendimento conversacional com memoria e grounding
- **Validacao de manifesto** (JSON/CSV) com relatorio aprovado/reprovado e CLI para CI
- **API HTTP de agendamento de docas** (Javalin + Postgres + storage) pronta para cloud AWS
- Testes unitários (JUnit 5)
- Pipeline **GitHub Actions** (test + smoke + artefato)

Cases: Escola Mundo Dev — Módulo 2 + Modelos & Inference + Prompt Engineering + DevOps Fundamentals + Cloud para Backend + Design de Software Inter + Banco de Dados Inter + Data Engineering & Pipelines Basico + Pipelines Intermediario.

## Um passeio pelo sistema

O FilaFacil é o balcão de uma transportadora pequena, escrito em Java. Quem opera no dia a dia abre um menu no terminal: cadastra coleta, olha a fila, muda status, pede para uma IA classificar um recado e confere se um manifesto de carga está inteiro. Ao lado disso existe uma segunda porta, sem tela, só HTTP: a API de agendamento de docas. Os dois programas moram no mesmo repositório e sobem com comandos diferentes. Dá para estudar e demonstrar tudo sem cartão de crédito. O CEP usa a ViaCEP, que é pública. O banco pode ficar só na memória. A IA desta máquina aponta para a Groq, no plano gratuito, com modelos compatíveis com o protocolo da OpenAI.

O arquivo `.env` na raiz (ele não vai para o Git) liga o modo HTTP da IA. Hoje ele está assim, sem a chave, que fica só na sua máquina:

| Variável | Valor em uso |
|----------|----------------|
| `FILAFACIL_LLM_MODE` | `http` |
| `FILAFACIL_LLM_BASE_URL` | `https://api.groq.com/openai/v1` |
| `FILAFACIL_LLM_MODEL_LIGHT` | `openai/gpt-oss-20b` |
| `FILAFACIL_LLM_MODEL_DETAILED` | `openai/gpt-oss-120b` |

Quem sobe o menu com `mvn -q compile exec:java` vê na primeira linha `LLM mode: http` e os dois nomes de modelo. Se essas variáveis não existirem, o programa continua no mock: uma resposta local, sem rede, útil para os testes automáticos.

Um detalhe que evita surpresa: só a camada de inferência lê o `.env`. A API, a auditoria e os pipelines ainda leem o ambiente do processo. Para um ensaio sem Docker, defina no PowerShell, antes do comando:

```powershell
$env:FILAFACIL_DB_URL = "memory"
```

Com isso o Postgres, o Redis e o Mongo não precisam estar ligados. Os dados de auditoria nascem de um seed equivalente ao SQL, e o pipeline grava CSV e JSON em `data/pipeline` em vez de tentar o banco.

### O que aconteceu numa rodada real

No dia 10 de novembro de 2026 o menu inteiro foi percorrido com a Groq de verdade, e a API foi exercitada à parte, em memória, na porta 8095 (a 8080 já estava ocupada nesta máquina). O `mvn test` continua verde e não gasta cota: o eval offline usa o gateway mock de propósito.

**Cadastro.** A opção 1 pergunta nome, endereço, pacotes, prioridade e CEP. Ela só grava depois que a ViaCEP responde. Nesta rodada entrou a coleta da Maria Silva, dois pacotes, prioridade alta, CEP `01310100`. A ViaCEP devolveu Bela Vista, São Paulo, SP, e o registro ganhou o id 3, porque o arquivo `data/coletas.json` já tinha outras coletas. Listar mostrou a fila. Filtrar por status `pendente` devolveu quem ainda não tinha saído. Atualizar status leva um id e um dos valores `pendente`, `em_rota` ou `concluida`. O resumo, em seguida, contou 3 coletas e 7 pacotes, com a divisão por prioridade e por status. O arquivo local foi restaurado ao fim do ensaio, para a coleta de demonstração não ficar misturada com o que já estava salvo.

**Triagem.** A opção 6 pede um texto e uma linha vazia para enviar. Uma frase curta, sem palavra de urgência, vai para o modelo leve. O texto usado foi: a coleta COL-10 da Maria está pendente e alguém quer confirmar o bairro. A primeira tentativa esbarrou num erro de certificado da JVM (`PKIX path building failed`), daqueles que aparecem quando um antivírus inspeciona HTTPS. O cliente tenta de novo. A segunda chamada voltou em cerca de 1 segundo, perfil LIGHT, modelo `openai/gpt-oss-20b`, sem fallback. O JSON passou no contrato: `consulta_entrega`, cliente Maria, código COL-10, sentimento neutro, prioridade sugerida média, intervenção humana falsa, confiança 0,9. As tools locais rodaram em seguida. Como COL-10 não existe no arquivo de coletas, o status da entrega veio `desconhecido` e a prioridade calculada pela tool ficou `baixa`. A IA sugere; o cadastro local é quem confirma o fato.

Uma reclamação longa muda de modelo sozinha. O roteador olha o texto que realmente segue para o modelo (a frase mais o resumo da sessão, quando houver). Se passar de 280 caracteres, ou se aparecer algo como reclamação, “não chegou”, urgente, danificado ou Procon, o perfil vira DETAILED e o modelo é o `openai/gpt-oss-120b`. A frase “o pedido não chegou, o pacote veio danificado, o cliente quer o Procon” caiu nesse perfil e a Groq respondeu em cerca de 1,6 segundo. O JSON veio incompleto: faltou a chave `justificativaIntervencencao`. O validador recusou, o menu mostrou `ERRO [inference]` e continuou de pé, com o último erro visível no topo. Esse nome de campo, no contrato do projeto, está grafado com um “c” a mais. O modelo leve copiou a chave do prompt e passou. O modelo grande omitiu a chave e foi barrado. A tela não aceita uma resposta bonita que fuja do contrato.

O número `costUsd~` no log é uma estimativa local (`FILAFACIL_LLM_PRICE_PER_1K`), para comparar perfil leve e detalhado. Não é a fatura da Groq. No plano gratuito a chamada entra na cota.

**Atendimento.** A opção 7 é uma conversa, não um formulário cego. Se faltam código, cliente ou status, o programa pergunta uma coisa de cada vez e ainda não chama a IA. Foi o que aconteceu com “quero consultar uma entrega”: veio a pergunta do código, depois a do cliente, depois a do status. As respostas `coleta COL-99`, `cliente: Joao Lima` e `status pendente` preencheram a sessão. Só então a Groq foi chamada. Duas tentativas voltaram sem texto útil (`content` vazio, coisa que esses modelos às vezes fazem quando gastam a resposta em raciocínio interno). A terceira veio certa, no modelo leve, em menos de 1 segundo: consulta da COL-99, cliente Joao Lima, status pendente, próxima ação “confirmar status com o cliente”, `conclusaoPronta: true`.

Quando a primeira frase já traz os três dados, a conversa não enrola. “Consulta da coleta COL-22. Cliente: Ana Souza. Status pendente. Ela quer saber se ja saiu.” foi direto ao modelo. O pacote da mensagem mais o resumo da sessão passou de 280 caracteres, então o perfil foi o detalhado. A conclusão estruturada saiu com código COL-22, cliente Ana Souza, status pendente e a próxima ação de responder se a coleta já saiu. O grounding aceitou, porque o status dito estava na própria frase. O programa se recusa a tratar como fato um status que ninguém escreveu.

`/reset` limpa a sessão. `/fim` sai sem concluir.

**Manifesto.** A opção 8 lê um JSON ou CSV e diz se a carga pode seguir. `data/manifestos/valido.json` fechou com 2 registros, 0 erros, status aprovado. `data/manifestos/invalido.json` fechou reprovado, 3 registros e 5 problemas: pacote PKG-100 duplicado, rota, veículo e responsável vazios nessa mesma linha, e status vazio no PKG-101. O mesmo julgamento existe sem menu, para o CI:

```powershell
mvn -q compile exec:java@manifest-cli "-Dexec.args=data/manifestos/valido.json"
```

**Auditoria.** A opção 10 abre um submenu em cima de um conjunto de entregas e reembolsos. No modo memória o seed traz 8 entregas e 4 reembolsos. A listagem pediu a página 1 com 5 itens e devolveu `Total=8`, `pagina=1/2`, `next=true`, da entrega 1 (Cliente Alpha, Motorista Ana, entregue) até a 5. Os joins contaram 8 linhas no INNER, 4 entregas sem ocorrência no LEFT, e no RIGHT o motorista “sem entregas” apareceu com zero. Aprovar o reembolso 1 mudou o status para `aprovado`. O resumo cacheado, na primeira leitura, veio da conta fresca (`fromCache=false`): 8 entregas, 1 pendente, 2 aprovados. A leitura seguinte, feita na hora, veio do cache (`fromCache=true`) com os mesmos números. O evento ficou registrado como `REEMBOLSO_APROVADO` no reembolso 1, usuário `terminal-local`.

**Pipelines.** A opção 11 leu as fixtures (coletas, ocorrências, entregas e o cadastro de clientes), montou a camada bruta e a processada por data e id de execução, e gravou a consolidada em arquivo. O log desta rodada: 17 registros lidos, 3 descartados na transformação, 10 gravados, status SUCCESS. Ingestão levou 254 ms, transformação 6 ms, carga 4 ms. Como o banco estava em memória, a carga registrou `cadastroJdbc=false` e `db=fallback-arquivo-only`. A opção 12, no modo incremental, leu 4 fontes (5 entregas, 4 motoristas, 4 clientes, 4 coletas de API) e consolidou 13 inseridos, 3 atualizados, 1 deletado e 3 deduplicados, sem evento atrasado. O lineage ficou em `data/pipeline/lineage` e o estado consolidado em `data/pipeline/consolidada/inter/state.json`.

**API de docas.** Ela sobe com `mvn -q compile exec:java@api`. Nesta prova a porta foi 8095 e o repositório era memória, então nada foi para Postgres nem para S3. O anexo foi para disco, no diretório `data/anexos-teste`.

| Chamada | Resultado |
|---------|-----------|
| `GET /health` | `{"status":"ok"}` |
| `POST /agendamentos` com TransLog, placa `abc1d23`, doca D1, `2026-11-12T10:00:00` | 201, id 1, placa normalizada para `ABC1D23`, status `agendado` |
| Segundo POST, Rota Sul, doca D2, dia 13 | 201, id 2 |
| `GET /agendamentos` | os dois registros |
| `GET /agendamentos?data=2026-11-12` | só o id 1 |
| `PATCH /agendamentos/1/status` com `em_atendimento` | 200, status atualizado |
| `POST /agendamentos/1/anexo` com um arquivo | 200, `anexoRef` em `file://.../agendamentos/1/...` |
| `POST /agendamentos/2/cancelar` | 200, status `cancelado` |
| POST sem transportadora | 400, `{"tipo":"validacao","mensagem":"Campo obrigatorio ausente: transportadora"}` |

O corpo guarda a referência do arquivo, não o arquivo em si. Em `FILAFACIL_API_MODE=prod` essa referência passaria a ser `s3://...`, e aí sim entra conta AWS. Para o uso local, o disco resolve.

### Como repetir

No terminal, com o `.env` já apontando para a Groq:

```powershell
$env:FILAFACIL_DB_URL = "memory"
mvn -q compile exec:java
```

As opções 6 e 7 são as que falam com a IA. As outras funcionam mesmo se a chave estiver vazia. A API, noutro terminal:

```powershell
$env:FILAFACIL_DB_URL = "memory"
mvn -q compile exec:java@api
```

Saúde em `http://localhost:8080/health`. Se a porta estiver ocupada, mude `FILAFACIL_API_PORT`.

Para voltar ao Gemini, que também tem cota gratuita e fala o mesmo protocolo, troque no `.env` a chave, a URL `https://generativelanguage.googleapis.com/v1beta/openai` e os modelos `gemini-2.5-flash` (leve) e `gemini-2.5-pro` (detalhado). A chave da Groq e a do Gemini ficam só no `.env`. Como as duas já apareceram numa conversa, o caminho seguro é revogar as atuais nos painéis e colar chaves novas.

## Pré-requisitos

- JDK 17+ (o Maven do ambiente tipicamente já usa 17)
- Apache Maven 3.9+
- Acesso à internet para a ViaCEP (cadastro real)
- (Opcional) chave de API OpenAI-compatible para modo HTTP
- (API cloud) Docker para Postgres local; conta AWS para deploy

## Executar

```powershell
# Padrao: modo mock (sem chave / sem rede de LLM)
mvn -q compile exec:java
```

### API de agendamento de docas

```powershell
# 1) Banco local (requer Docker Desktop)
docker compose up -d

# 2) Env (copie .env.example -> .env; a API tambem le variaveis do shell)
# FILAFACIL_API_MODE=local FILAFACIL_API_PORT=8080 ...

# 3) Subir API
mvn -q compile exec:java@api

# Smoke sem Docker (repositorio em memoria):
$env:FILAFACIL_DB_URL = "memory"
mvn -q compile exec:java@api
```

Health: `GET http://localhost:8080/health`

## Testes

```powershell
mvn clean test
```

Os testes usam fake de `CepApiClient` e gateway LLM em mock (não chamam a rede). Na primeira vez apos mudar de JDK, use `clean` para recompilar.

### Eval offline (golden dataset)

```powershell
mvn -q exec:java@offline-eval
```

Ou via teste: `OfflineEvalRunnerTest` (incluido em `mvn test`).

## Variaveis de ambiente LLM (`FILAFACIL_LLM_*`)

Nunca coloque chaves reais no codigo ou no README.

| Variavel | Default | Descricao |
|----------|---------|-----------|
| `FILAFACIL_LLM_MODE` | `mock` | `mock` (sem rede) ou `http` |
| `FILAFACIL_LLM_API_KEY` | (vazio) | Bearer token do provedor (obrigatoria em `http`) |
| `FILAFACIL_LLM_BASE_URL` | `https://api.openai.com/v1` | Base OpenAI-compatible (OpenAI, Groq, etc.) |
| `FILAFACIL_LLM_MODEL_LIGHT` | `gpt-4o-mini` | Perfil leve (triagem barata/rapida) |
| `FILAFACIL_LLM_MODEL_DETAILED` | `gpt-4o` | Perfil detalhado (casos criticos) |
| `FILAFACIL_LLM_TIMEOUT_SECONDS` | `20` | Timeout HTTP |
| `FILAFACIL_LLM_MAX_RETRIES` | `2` | Retries em falhas transitorias |
| `FILAFACIL_LLM_PRICE_PER_1K` | `0.00015` | Estimativa de custo USD / 1k tokens |

Exemplo PowerShell (HTTP):

```powershell
$env:FILAFACIL_LLM_MODE = "http"
$env:FILAFACIL_LLM_API_KEY = "sua-chave"
$env:FILAFACIL_LLM_BASE_URL = "https://api.openai.com/v1"
mvn -q compile exec:java
```

## Comparacao de perfis de modelo

| Dimensao | LIGHT (`MODEL_LIGHT`) | DETAILED (`MODEL_DETAILED`) |
|----------|------------------------|------------------------------|
| Uso | Consultas simples / baixa criticidade | Reclamacoes, urgencia, texto longo |
| Latencia | Menor (prioridade) | Maior |
| Custo | Menor | Maior |
| Qualidade | Suficiente para classificacao | Melhor em ambiguidades |
| Temperature | ~0.4 | ~0.1 (previsibilidade) |

Roteamento automatico: heuristica local (palavras de urgencia/reclamacao e tamanho do texto). Sem escolha manual no fluxo padrao. Fallback: se o perfil leve falhar, tenta o detalhado.

## API REST utilizada

- **ViaCEP:** `https://viacep.com.br/ws/{cep}/json/`
- No cadastro, o sistema pede o CEP, consulta a API com `CompletableFuture` e só persiste após sucesso.
- Em falha (CEP inválido/rede), o cadastro **não** é gravado e o menu continua.

## Persistência

Arquivo: `data/coletas.json`

Campos: `id`, `nome`, `endereco`, `pacotes`, `prioridade`, `status`, `cep`, `bairro`, `cidade`, `uf`

## Menu

| Opção | Ação |
|-------|------|
| 1 | Cadastrar (com CEP / ViaCEP) |
| 2 | Listar |
| 3 | Filtrar (status ou prioridade) |
| 4 | Atualizar status |
| 5 | Resumo operacional |
| 6 | Triagem IA (mensagem → JSON validado) |
| 7 | Atendimento conversacional (multi-turno + clarificacao) |
| 8 | Validar manifesto (JSON/CSV + relatorio) |
| 9 | Deletar coleta |
| 10 | Auditoria entregas/reembolsos (submenu) |
| 11 | Pipeline batch (ETL multi-fonte) |
| 12 | Pipeline inter (CDC/incremental) |
| 0 | Sair |

No atendimento conversacional: `/reset` limpa a sessao; `/fim` encerra sem concluir.

### Valores

- **Prioridade:** `baixa`, `media`, `alta`
- **Status:** `pendente`, `em_rota`, `concluida`

## Prompt library e few-shot

Resources versionados:

- `src/main/resources/inference/prompts/` — `classify`, `extract`, `summarize`, `internal-reply`, `transform`
- `src/main/resources/inference/fewshot/catalog.json` — exemplos positivo / negativo / contraexemplo / escalacao (dados ficticios)

Cada prompt declara objetivo, contexto, restricoes, criterios e formato de saida. O `PromptComposer` monta system prompt = template + few-shot + memoria da sessao.

## Fluxo conversacional e grounding

1. Recepcao: identifica intencao e campos ja informados
2. Clarificacao: pergunta so o que falta (sem repetir)
3. Conclusao: JSON estruturado so com campos preenchidos ou marcados desconhecido
4. Grounding: nao inventar status/prazo/codigo; origem `fato|inferencia|ausente`; retry orientado (max 2) ou revisao humana

## Validacao de manifesto (DevOps)

Fixtures locais:

- `data/manifestos/valido.json` / `valido.csv`
- `data/manifestos/invalido.json` (duplicata + campos vazios)

Regras: campos obrigatorios, pacote unico, rota/veiculo/responsavel presentes. Relatorio com status `aprovado` ou `reprovado`.

### CLI (CI / headless)

```powershell
# Exit 0 = aprovado
mvn -q compile exec:java@manifest-cli "-Dexec.args=data/manifestos/valido.json"

# Exit 1 = reprovado
mvn -q exec:java@manifest-cli "-Dexec.args=data/manifestos/invalido.json"

# Salvar relatorio
mvn -q exec:java@manifest-cli "-Dexec.args=data/manifestos/valido.json --out data/manifestos/ultimo-relatorio.txt"
```

No menu interativo: opcao **8**.

## Cloud Backend (API de docas)

API HTTP separada do terminal (`com.mundodev.filafacil.api.ApiMain`). Sem frontend e sem auth completa.

### Endpoints

| Metodo | Rota | Acao |
|--------|------|------|
| GET | `/health` | disponibilidade |
| POST | `/agendamentos` | criar (transportadora, placa, doca, dataHora) |
| GET | `/agendamentos` | listar |
| GET | `/agendamentos?data=YYYY-MM-DD` | filtrar por data |
| PATCH | `/agendamentos/{id}/status` | atualizar status |
| POST | `/agendamentos/{id}/cancelar` | cancelar |
| POST | `/agendamentos/{id}/anexo` | multipart `file` (limite ~2 MB) |

Status: `agendado`, `em_atendimento`, `concluido`, `cancelado`.

### Variaveis de ambiente (`FILAFACIL_API_*`)

Nunca commite `.env` com secrets. Use `.env.example` como modelo.

| Variavel | Default local | Descricao |
|----------|---------------|-----------|
| `FILAFACIL_API_MODE` | `local` | `local` (filesystem anexos) ou `prod` (S3) |
| `FILAFACIL_API_PORT` | `8080` | Porta HTTP |
| `FILAFACIL_DB_URL` | `jdbc:postgresql://localhost:5432/filafacil` | JDBC Postgres (RDS em prod); use `memory` para smoke sem Docker |
| `FILAFACIL_DB_USER` | `filafacil` | Usuario DB |
| `FILAFACIL_DB_PASSWORD` | `filafacil` | Senha DB (so via env/secret) |
| `FILAFACIL_S3_BUCKET` | (vazio) | Bucket S3 obrigatorio em `prod` |
| `FILAFACIL_AWS_REGION` | `us-east-1` | Regiao AWS |
| `FILAFACIL_ANEXOS_DIR` | `data/anexos` | Dir local de anexos em `local` |
| `FILAFACIL_MAX_ANEXO_BYTES` | `2097152` | Limite de upload |

Storage: em `local` grava em disco e salva `anexo_ref` tipo `file://...`; em `prod` envia ao S3 e salva `s3://bucket/key`. O banco guarda **somente a referencia**.

### Deploy AWS (checklist dos 8 requisitos)

1. **Env**: configure as vars acima no servico (App Runner / ECS). Secrets de DB via Secrets Manager.
2. **Banco**: RDS Postgres; security group permitindo a origem da API; schema em `src/main/resources/api/schema.sql`.
3. **Storage**: bucket S3; IAM so no prefixo `agendamentos/*` (ver `infra/iam-api-role-policy.json`).
4. **Deploy**: empacote o jar (`mvn -B package`) e publique no **AWS App Runner** (padrao) apontando para `ApiMain`, ou **ECS Fargate**.
5. **Load balancer**: App Runner expoe URL HTTPS com LB gerenciado; alternativa ECS + **ALB** com target no container e health `GET /health`.
6. **Auto scaling**: min=1 max=3, metrica CPU ~70% — ver `infra/auto-scaling.json`.
7. **IAM**: role da tarefa/servico com policy minima (DB secret, S3 anexos, CloudWatch Logs) — sem admin amplo.
8. **Logs**: stdout da JVM (slf4j-simple) coletado no CloudWatch; registrar startup, method/path, erros DB/S3 e health **sem** senhas/tokens/PII.

Checklist pos-deploy: `curl https://<url-publica>/health` deve retornar `{"status":"ok"}`.

Detalhes extras: `infra/README.md`.

## CI vs deploy

| Fluxo | O que faz | Onde |
|-------|-----------|------|
| **CI** | `mvn test` + smoke do `ManifestCli` no fixture valido | GitHub Actions em todo push/PR (`.github/workflows/ci.yml`) |
| **Artefato** | Apos testes verdes: `mvn package`, zip jar + fixtures + README, `upload-artifact` | Job `artifact` do mesmo workflow |
| **Release tag** | Em tags `v*` publica zip no GitHub Release | Soft — precisa remote GitHub e `GITHUB_TOKEN` |
| **Deploy cloud API** | App Runner/ECS + RDS + S3 (manual/conta AWS) | Documentado acima; nao automatizado no Actions deste case |

### Conferir Actions

1. Abra o repositorio no GitHub → aba **Actions**.
2. Selecione o workflow **CI** e o run do commit/PR.
3. Confira jobs `test` (unitarios + smoke) e `artifact` (download do zip).

### Tag de versao (apos merge)

```powershell
git tag v1.1.0-manifest
git push origin v1.1.0-manifest
```

O push da tag dispara o job `release` (quando o remote for GitHub).

Equivalente local ao CI:

```powershell
mvn -B test
mvn -q exec:java@manifest-cli "-Dexec.args=data/manifestos/valido.json"
mvn -B package -DskipTests
```

## Banco de Dados Inter (auditoria)

Painel **terminal** de auditoria de entregas/reembolsos (sem web/API propria). Opcao de menu **10**.

### Infra local

```powershell
docker compose up -d
# sobe Postgres (schema API + aud_* + seeds), Redis e MongoDB
```

Fallback sem Docker: `$env:FILAFACIL_DB_URL="memory"` (store em memoria com o mesmo seed).

### Modelo relacional (`aud_*`)

| Tabela | Papel |
|--------|-------|
| `aud_clientes` | clientes |
| `aud_motoristas` | motoristas |
| `aud_entregas` | entregas (FK cliente/motorista) |
| `aud_ocorrencias` | ocorrencias opcionais por entrega |
| `aud_reembolsos` | reembolsos por entrega |

Scripts: `src/main/resources/auditoria/02-auditoria-schema.sql`, `03-auditoria-seed.sql`.

### Indices (justificativa)

| Indice | Campo | Por que |
|--------|-------|---------|
| `idx_aud_entregas_status` | status | filtro frequente na listagem |
| `idx_aud_entregas_data` | data_entrega | filtro por periodo |
| `idx_aud_entregas_cliente` | cliente_id | join/filtro por cliente |
| `idx_aud_entregas_motorista` | motorista_id | join/filtro por motorista |
| `idx_aud_reembolsos_status` | status | contagem/aprovacao |
| `idx_aud_reembolsos_entrega` | entrega_id | join com entregas |

### Joins / paginacao / transacao / cache / NoSQL

- **INNER JOIN** entregas + clientes + motoristas
- **LEFT JOIN** ocorrencias (entrega sem ocorrencia continua na lista)
- **RIGHT JOIN** motoristas (inclui motorista sem entregas) — demo no submenu
- Listagem paginada: pagina + tamanho + total
- Aprovacao de reembolso em transacao (`BEGIN`/`COMMIT`/`ROLLBACK`)
- Redis: chave `filafacil:auditoria:resumo:v1`, TTL 60s, invalidacao na aprovacao
- MongoDB collection `audit_events` (ou memoria se Mongo offline): tipo, usuario, entidade, dados antigos/novos

### Submenu 10

1. Listar entregas paginadas  
2. Demo joins  
3. Aprovar reembolso  
4. Resumo cacheado  
5. Eventos NoSQL  

## Data Engineering — Pipeline batch

Pipeline local em Java (`com.mundodev.filafacil.pipeline`) com etapas **ingest → transform → load**, camadas **bruta / processada / consolidada**, particionamento por `dt` + `runId`, retries e logs.

### Fontes (fixtures classpath → `data/pipeline/input`)

| Fonte | Formato | Conteudo |
|-------|---------|----------|
| coletas | CSV | codigo, cliente, regiao, status, prazos, data |
| ocorrencias | JSON | tipo, descricao, referencia, data, status |
| entregas | Parquet | id, regiao, datas, status, atraso, cliente, unidade |
| clientes/unidades | tabela `pipe_clientes_unidades` (Postgres) ou CSV fallback | cadastro para join |

Fixtures canonicos: `src/main/resources/pipeline/fixtures/`. Parquet gerado via DuckDB JDBC a partir do CSV de entregas.

### Camadas e saidas

```
data/pipeline/
  input/
  bruta/dt=YYYY-MM-DD/run=<id>/
  processada/dt=.../run=.../
  consolidada/dt=.../run=.../   # metricas.json + metricas.csv
  logs/run-<id>.log
```

### Como rodar

```powershell
# Via menu: opcao 11
mvn -q compile exec:java

# Via CLI
mvn -q compile exec:java@pipeline

# Sem Postgres (cadastro/metricas so em arquivo)
$env:FILAFACIL_DB_URL = "memory"
mvn -q compile exec:java@pipeline
```

Com Docker: `docker compose up -d` aplica `04-pipeline-schema.sql` + `05-pipeline-seed.sql` e a carga tenta upsert em `pipe_metricas_diarias`. Se o banco estiver offline, o pipeline segue com fallback CSV/arquivo (documentado no log).

### Metricas

Agregacoes por data, regiao, status e cliente: totais de coletas, entregas, atrasos e ocorrencias. Registros invalidos sao descartados com contagem (sem perda silenciosa).

Env: `FILAFACIL_PIPELINE_ROOT` (default `data/pipeline`), `FILAFACIL_PIPELINE_RETRIES` (default `2`).

## Data Engineering — Pipeline intermediario (CDC)

Evolucao do batch basico em `com.mundodev.filafacil.pipeline.inter`: carga **batch** ou **incremental**, CDC simulado, upsert/dedup, checkpoints por fonte, late arriving, lineage, Parquet particionado na processada e paralelismo entre fontes.

### Fontes (4)

| Fonte | Fixture | Controle | Prioridade |
|-------|---------|----------|------------|
| API coletas (mock) | `fixtures/inter/coletas_api.json` | `updated_at` / `version` | 30 |
| Clientes (relacional/CSV) | `fixtures/inter/clientes.csv` | `updated_at` | 40 |
| Entregas CSV/Parquet | `fixtures/inter/entregas.csv` (+ parquet) | `updated_at` | 20 |
| Motoristas/rotas JSON | `fixtures/inter/motoristas_rotas.json` | `updated_at` / `deleted` | 10 |

### Comportamento

- **Incremental:** le apenas registros com `updated_at` > checkpoint da fonte
- **Dedup/merge:** chave `entity|business_key`; vence maior `updated_at`, depois `version`, depois prioridade da fonte; versao antiga = `SKIP_STALE`
- **Late arriving:** `FILAFACIL_PIPELINE_LATE_POLICY=mark|reject|accept` (default `mark`)
- **Checkpoints:** `data/pipeline/checkpoints/checkpoints.json` (por fonte; so avanca em sucesso)
- **Lineage:** `data/pipeline/lineage/lineage-<runId>.jsonl`
- **Processada:** Parquet particionado por `dt` + `regiao` em `processada/inter/run=<id>/`
- **Schema SQL:** `06-pipeline-inter-schema.sql` (`pipe_checkpoints`, `pipe_lineage`)

### Como rodar

```powershell
# Menu opcao 12
mvn -q compile exec:java

# CLI incremental
mvn -q compile exec:java@pipeline "-Dexec.args=--mode incremental"

# CLI batch inter + reprocessamento de periodo
mvn -q compile exec:java@pipeline "-Dexec.args=--mode batch --reprocess --from 2026-11-01T00:00:00 --to 2026-11-30T23:59:59"

# Uma fonte
mvn -q compile exec:java@pipeline "-Dexec.args=--mode incremental --source entregas"
```

## Design de Software (dominio coletas)

Organizacao **feature-based** em `com.mundodev.filafacil.coletas`:

```
coletas/
  Coleta.java
  ColetaService.java          # coordena; DI apenas (sem new de deps)
  factory/ColetaFactory.java  # Factory: coleta valida + status pendente
  priority/
    PriorizacaoStrategy.java
    PrioridadeManualStrategy.java      # default no Main
    PrioridadePorPacotesStrategy.java  # >=10 alta, >=5 media, senao baixa
  observer/
    ColetaObserver.java
    ColetaEventPublisher.java
    LogOperacionalObserver.java
    ContadorMemoriaObserver.java
  repo/
    ColetaRepository.java
    FileColetaRepository.java
    InMemoryColetaRepository.java      # testes
```

| Pattern | Onde |
|---------|------|
| Factory | `ColetaFactory.criar(...)` usada no cadastro |
| Strategy | `PriorizacaoStrategy` injetada; troque no wiring do `Main` |
| Observer | `ColetaEventPublisher` notifica em criar/atualizar status |
| DI | `Main` monta repository/factory/strategy/publisher/CEP e injeta no service |

Contratos: `ColetaRepository`, `PriorizacaoStrategy`, `ColetaObserver`, `CepApiClient`. Testes em `src/test/java/.../coletas/` usam fakes (sem arquivo real).

## Arquitetura

```
controller.MenuController
  → coletas.ColetaService
      → coletas.repo.ColetaRepository (File | InMemory)
      → coletas.factory.ColetaFactory
      → coletas.priority.PriorizacaoStrategy
      → coletas.observer.ColetaEventPublisher
      → state.AppState
      → integration.CepApiClient (ViaCEP)
  → inference.OperationalTriageService (one-shot)
  → inference.prompt.ConversationOrchestrator (multi-turno)
      → PromptLibrary / FewShotCatalog / PromptComposer
      → ConversationSession + ClarificationEngine
      → GroundingValidator + OrientedRetry
      → FallbackInferenceGateway (Mock | HttpOpenAI-compatible)
  → manifest.ManifestValidationService / ManifestCli
  → pipeline.PipelineModule (menu 11 / PipelineCli)
  → pipeline.inter.PipelineInterModule (menu 12 / --mode)

api.ApiMain (entrypoint separado)
  → AppConfig (env)
  → JdbcAgendamentoRepository (Postgres)
  → AttachmentStorage (local filesystem | S3)
  → AgendamentoService → Javalin routes (/health, /agendamentos)
```

Schema triagem: `src/main/resources/inference/triage-schema.json`  
Schema API: `src/main/resources/api/schema.sql`

## Analise de falhas (exemplo)

Se o golden dataset marcar divergencia em `tipoSolicitacao` ou `prioridadeSugerida`:

1. Conferir se a mensagem do caso cobre as palavras-chave do mock/router.
2. Ajustar heuristica em `MockInferenceGateway` / `ModelRouter` (nao so o expected).
3. Reexecutar `OfflineEvalRunner` e registrar impacto operacional (triagem errada → acao/prioridade incorreta).

## Estrutura

```
src/main/java/com/mundodev/filafacil/
  Main.java
  controller/MenuController.java
  coletas/
    ColetaService.java
    factory/
    priority/
    observer/
    repo/
  state/AppState.java
  integration/...
  inference/
    InferenceFactory.java
    OperationalTriageService.java
    ModelRouter.java
    MockInferenceGateway.java
    HttpOpenAiCompatibleGateway.java
    FallbackInferenceGateway.java
    TriageSchemaValidator.java
    tools/
    eval/OfflineEvalRunner.java
  manifest/
    ManifestCli.java
    ManifestValidationService.java
    ManifestValidator.java
    ManifestReader.java
  api/
    ApiMain.java
    AppConfig.java
    ApiApp.java
    service/AgendamentoService.java
    repo/
    storage/
  pipeline/
    PipelineModule.java / PipelineCli.java / PipelineOrchestrator.java
    stage/ IngestStage TransformStage LoadStage
    io/ ParquetIO FixtureMaterializer SourceReaders
    store/CadastroRepository.java
  model/...
  error/...
src/main/resources/pipeline/
data/pipeline/   # gerado em runtime (gitignore)
docker-compose.yml
.env.example
infra/
.github/workflows/ci.yml
src/test/resources/inference/golden/
src/test/java/.../inference/
src/test/java/.../manifest/
src/test/java/.../api/
src/test/java/.../pipeline/
```
