# Design

## Context

O índice em `cloudai.vector.LocalVectorIndex` grava o documento inteiro, num único domínio `operacao`, e devolve um hit acima do corte 0.55. Não há chunk, filtro de acesso, reranking, citação nem memória por operador. A conversa do menu (`ConversationOrchestrator`) só completa campos de triagem. Ver `proposal.md` — Why. Requisitos: specs deste change.

O embedder local `TokenEmbedder` já existe e não chama rede. O prazo é de cerca de 6 horas e o CI não pode depender de API de embedding.

## Goals / Non-Goals

**Goals:**

- Um comando local que ingere, pergunta e avalia, com exit code utilizável em teste
- Índice em `data/rag/`, separado do índice da triagem cloud
- Resposta citável sem chamada de modelo pago

**Non-Goals:**

- Trocar o índice, o menu ou o golden de 3 casos já existentes
- Subir pgvector, Pinecone ou um serviço de embedding
- Gerar a resposta final com Groq ou OpenAI

## Decisions

### 1. CLI `RagMain` via `exec:java@rag`

Comandos: `ingest`, `ask --session <id> --role <operador|parceiro> --text <texto>`, `eval`.

- **Por que:** o case é um assistente à parte da triagem; os outros módulos novos já usam um main próprio
- **Alternativa:** opção 13 no menu — rejeitada para não mexer no contrato 0–12
- **Alternativa:** reusar `CloudAiMain` — rejeitada porque aquele fluxo é fila de triagem, não pergunta com citação

### 2. Corpus e chunk em arquivo

Sete documentos curtos em `src/main/resources/rag/corpus/`, cada um com as seções e os metadados do spec. `rag/retrieval.json` guarda `chunkSize`, `overlap`, `topK`, `hybridWeight`, `rerank` e `sessionTtlMinutes`.

O corte de caractere usa janela e overlap. A chave de deduplicação é versão + seção + texto normalizado. Publicar a versão 2 de um documento apaga só os chunks daquele `docId` e regrava o vetor desses chunks.

- **Por que:** a atualização incremental é observável num teste que compara o vetor de um documento que não mudou
- **Alternativa:** reindexar a pasta inteira a cada versão — rejeitada pelo requisito de não recriar a base
- **Alternativa:** chunk por frase com biblioteca nova — rejeitada no prazo; a janela de caracteres cabe no teste de chunk curto

Valores iniciais: chunk 240, overlap 40, top-k 3, peso híbrido 0.35, rerank ligado, TTL 30 minutos. O teste de chunk ruim usa 30 só naquela execução.

### 3. Busca em cima do `TokenEmbedder`

Cada chunk guarda o vetor de `TokenEmbedder.embed`. A busca híbrida soma cosseno e a fração de tokens da pergunta presentes no chunk. Com peso 0, fica só o vetor. O reranking soma um bônus quando os tokens da seção aparecem na pergunta e então corta no top-k.

Namespaces são os domínios do corpus (coletas, entregas, ocorrencias, atendimento, comprovantes, atraso, parceiros). Filtro de acesso: `operador` lê `publico` e `operador`; `parceiro` lê `publico` e `parceiro`.

- **Por que:** reaproveita o embedder que já segurou o corte da triagem, sem rede
- **Alternativa:** API de embedding — rejeitada pelo custo e pelo CI offline
- **Alternativa:** copiar a função de hash — rejeitada para não divergir o vetor no futuro

O corte efetivo continua configurável. O híbrido existe para um termo raro que o hash sozinho deixa abaixo do corte, que é a falha já vista com colisão de bucket.

### 4. Resposta montada, não gerada por LLM

Se não houver chunk, a resposta é a frase fixa de ausência, sem citação e sem regra. Se houver, a resposta tem duas partes: `evidencia` (texto do chunk) e `orientacao` (frase fixa apontando a seção). A citação leva id do documento, versão e seção.

A intenção é um mapa de termos para domínio (atraso, comprovante, coleta, entrega, ocorrência, faq, parceiro). Sem termo, a busca usa o último domínio da sessão se o TTL não venceu. Sem isso, percorre os namespaces.

- **Por que:** groundedness fica determinística e o `mvn test` não precisa de chave
- **Alternativa:** passar os chunks ao gateway Groq — rejeitada como caminho padrão; o case aceita resposta rastreável, e a citação é o critério

### 5. Memória em arquivo por sessão

`data/rag/sessions/<id>.json` guarda resumo, último domínio, fatos esclarecidos e o instante. O relógio entra por parâmetro no teste. Sessões diferentes não compartilham arquivo. A memória só preenche filtro. A regra escrita na sessão não entra na evidência se o índice não a devolveu.

- **Por que:** isolamento e TTL são arquivos e um timestamp, fáceis de testar
- **Alternativa:** reusar `ConversationSession` do menu — rejeitada porque aquela sessão mistura campos de triagem e não tem TTL nem papel de acesso

### 6. Avaliação com falha injetada

`rag/questions.json` traz perguntas com chunk esperado ou com o rótulo `ausente`. O relatório calcula precision, recall, relevância média, cobertura, groundedness e taxa sem evidência.

Três execuções de diagnóstico, além da configuração boa:

- versão antiga ainda citada → `documento_desatualizado`
- chunk size 30 na pergunta do comprovante → `chunk_mal_dividido`
- filtro de acesso que esconde o único chunk → `filtro_incorreto`

O relatório grava chunk size, overlap, top-k, filtros e rerank, e diz qual falha o valor vigente evita. Não entra em `inference/golden`.

- **Por que:** o case pede a análise junto da métrica, e o gráfico ilegível não muda o fato de que chunk size e rerank são os testes de precisão
- **Alternativa:** um único percentual — rejeitada porque as classes de falha são critério explícito

## Risks / Trade-offs

- [Hash de token erra pergunta sem termo distintivo] → híbrido, rerank e testes com o termo da seção; o corte fica no JSON
- [Resposta sem prosa de modelo] → a evidência é o próprio chunk citado, que é o que o operador precisa rastrear
- [Pesos do PDF não saíram legíveis] → chunk size e rerank têm teste próprio de precisão

## Migration Plan

1. Acrescentar recursos, pacote e o execution `rag` no `pom.xml` sem alterar `LocalVectorIndex` nem o menu
2. `mvn test` cobre ingestão incremental, filtros, citação, ausência, duas sessões, TTL e as três falhas
3. Rollback de código: remover o pacote e o execution. `data/rag/` é local

## Open Questions

Nenhuma que mude spec, abordagem ou tarefas. Os números iniciais ficam em `retrieval.json`.
