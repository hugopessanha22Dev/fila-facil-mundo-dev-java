# Tasks

## 1. Corpus e ingestao

- [x] 1.1 Criar `src/main/resources/rag/corpus/` com os sete documentos fictícios (coletas, entregas, ocorrências, FAQ, comprovantes, atraso, parceiros), cada um com título, tipo, domínio, versão, data, owner, nível de acesso e seções; verificar teste que carrega as sete famílias e rejeita família desconhecida
- [x] 1.2 Implementar limpeza, chunk com `chunkSize` e `overlap` de `rag/retrieval.json`, deduplicação por versão+seção+texto e metadados de origem no chunk; verificar teste de chunk vazio descartado, de duplicata única e de cada chunk apontar documento, versão, seção e acesso
- [x] 1.3 Publicar a versão 2 de um documento substituindo só os chunks daquele id e regravando o vetor deles, sem alterar o vetor dos outros documentos; verificar teste que a busca ativa cita a versão 2 e que o vetor do documento intocado permanece igual

## 2. Busca, filtros e reranking

- [x] 2.1 Indexar cada chunk com `TokenEmbedder`, em namespace de domínio, e filtrar por tipo, versão, acesso e domínio, com top-k lido da configuração; verificar teste de parceiro sem chunk de operador, de domínio entregas sem chunk de coleta, e de top-k 1 devolvendo um item
- [x] 2.2 Combinar cosseno e termo quando `hybridWeight` for maior que zero, e reranquear pelo token da seção; verificar teste em que o híbrido recupera o termo que o vetor sozinho deixa abaixo do corte, e teste de pergunta ambígua em que o rerank coloca a seção certa na frente

## 3. Resposta citada e memoria

- [x] 3.1 Montar a resposta em evidência citada (documento, versão, seção) e orientação separada, direcionando o domínio pelos termos da pergunta; sem chunk no corte, declarar ausência e não inventar regra; verificar teste da pergunta objetiva com citação e da pergunta ausente sem fonte
- [x] 3.2 Gravar por sessão o resumo, o último domínio e os fatos esclarecidos, com TTL e arquivo isolado; a memória só preenche filtro e não vira evidência; verificar teste de duas sessões sem mistura, de TTL vencido sem reaplicar domínio, e de regra só na memória recusada como evidência

## 4. Avaliacao, CLI e documentacao

- [x] 4.1 Avaliar `rag/questions.json` com precision, recall, relevância, cobertura, groundedness e taxa sem evidência, classificando `documento_desatualizado`, `chunk_mal_dividido` (chunk size 30) e `filtro_incorreto`; o relatório lista chunk size, overlap, top-k, filtro e rerank vigentes; verificar essas métricas e as três classes num teste, sem gravar pergunta em `inference/golden`
- [x] 4.2 Expor `RagMain` em `exec:java@rag` com `ingest`, `ask` e `eval`, usando embedder local e sem chamada de API; verificar execução local de `ingest` e `ask` com mock de arquivo e que o menu não ganha opção 13
- [x] 4.3 Documentar no README o corpus, os comandos, o que a resposta cita, a recusa sem evidência e que o embedding é local; verificar que o comando do README bate com o entrypoint e que `mvn test` segue verde, inclusive o eval antigo de 3 casos

## Workflow follow-up

- Arquivar o change (`/opsx-archive`) depois da revisão.
