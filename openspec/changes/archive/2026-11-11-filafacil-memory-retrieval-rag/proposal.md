# Proposal

## Why

A FilaFácil já guarda quatro documentos inteiros num índice local de triagem, mas o operador ainda não consegue perguntar a uma base versionada e receber a regra com a seção, a versão e a prova de que ela veio do documento vigente. O case Mundo Dev **Memory & Retrieval RAG Básico + Intermediário** pede esse assistente em até 6 horas, sem inventar regra e sem misturar a memória de um operador com a de outro.

## What Changes

- Ingerir documentos operacionais fictícios com parsing, limpeza, deduplicação, chunking, metadados e atualização da versão sem reindexar a base inteira
- Gerar embedding local por chunk, em namespace de domínio, com filtros de tipo, versão, acesso e domínio
- Responder com busca top-k, busca híbrida, reranking, contexto citado e recusa quando não houver evidência
- Guardar memória de sessão com resumo, último domínio e TTL, isolada por operador, sem deixar a conversa substituir o documento
- Medir precision, recall, relevância, cobertura e groundedness, e registrar falha de documento velho, chunk ruim ou filtro errado
- Manter o menu 0–12, o índice da triagem cloud e o golden de 3 casos do eval antigo
- Premissa: embeddings locais e resposta montada a partir dos chunks, sem API paga de embedding e sem banco vetorial em nuvem. O gráfico de pesos não extraiu números; o texto dá maior peso a reranking e chunk size

## Capabilities

### New Capabilities

- `knowledge-ingestion`: documentos simulados viram chunks versionados, com metadados e troca incremental de versão
- `vector-retrieval`: busca semântica com namespace, filtros, top-k, híbrido e reranking
- `grounded-rag-answer`: intenção, contexto citado e recusa quando a regra não está na base
- `operator-session-memory`: histórico resumido por sessão, com expiração, sem mistura entre operadores e sem furar o grounding
- `retrieval-evaluation`: métricas de recuperação e diagnóstico das falhas de chunk, filtro e versão

### Modified Capabilities

- (nenhuma main spec arquivada; a triagem cloud e a conversa do menu permanecem)

## Impact

- Novo pacote e CLI (ex.: `com.mundodev.filafacil.rag`), sem opção nova no menu
- Corpus e perguntas de avaliação em `src/main/resources/rag/`, fora de `inference/golden`
- Índice em arquivo sob `data/rag/`, separado de `data/cloud-ai/`
- Reuso do embedder local já existente, sem nova dependência
- README com o comando de ingestão, pergunta e avaliação
- Referência: PDF *Memory & Retrieval RAG Básico + Intermediário – FilaFácil Logística* (Escola Mundo Dev)
