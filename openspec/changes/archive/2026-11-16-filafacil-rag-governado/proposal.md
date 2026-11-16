# Proposal

## Why

O RAG da FilaFácil já ingere sete documentos, busca híbrida, cita a fonte e isola a sessão pelo TTL. O case avançado pede governança que ainda não existe: status do documento, namespace histórico e de tenant, expansão de consulta, compressão de contexto, top-k conforme a complexidade e memória que não guarda dado sensível. O gráfico de pesos não extraiu números.

## What Changes

- Incluir o oitavo documento, o histórico operacional de decisões, junto das sete famílias já indexadas
- Registrar status vigente, expirado, substituído e em revisão, com hash do conteúdo e a versão anterior ou posterior
- Bloquear documento expirado e em revisão na resposta operacional, e mover o substituído para um namespace histórico
- Registrar cada execução de ingestão e reprocessar só os chunks do documento alterado
- Separar o índice por tenant, além do domínio, e filtrar vigência e nível de acesso
- Expandir a consulta com termos do mesmo domínio sem trocar a intenção
- Comprimir trechos redundantes e aumentar o top-k só quando a pergunta pede mais de um documento
- Guardar perfil operacional e preferência de formato na sessão, recusar dado sensível e impedir que um tenant leia outro
- Classificar falha de documento expirado, top-k insuficiente, expansão inadequada e reranking fraco, cada uma com uma ação corretiva
- Manter a frase `Nao encontrado nos documentos.`, o menu nas opções 0 a 12 e os três casos do eval antigo
- Premissa: índice local, sem fine-tuning e sem chamada paga

## Capabilities

### New Capabilities

- (nenhuma; o comportamento novo cabe nas specs de RAG já arquivadas)

### Modified Capabilities

- `knowledge-ingestion`: ganha a oitava família, o histórico operacional, além de status, hash, linhagem e namespace histórico
- `vector-retrieval`: namespace de tenant, filtro de vigência e expansão de consulta
- `grounded-rag-answer`: compressão de contexto e top-k dinâmico
- `operator-session-memory`: perfil, preferência de formato, isolamento por tenant e recusa de dado sensível
- `retrieval-evaluation`: quatro classes novas de falha, cada uma com ação corretiva

## Impact

- Extensão de `com.mundodev.filafacil.rag` e de `src/main/resources/rag/`, sem opção nova de menu
- O teste que exige sete famílias passa a exigir oito, no mesmo passo do documento novo
- A frase de ausência e o marcador do comprovante permanecem
- README com status do documento, namespace histórico e o motivo de não guardar dado sensível
- Referência: PDF *Memory & Retrieval RAG Básico + Intermediário + Avançado – FilaFácil Logística* (Escola Mundo Dev)
