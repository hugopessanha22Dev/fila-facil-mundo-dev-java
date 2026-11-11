# Proposal

## Why

A triagem com IA da FilaFácil já funciona no terminal (gateway OpenAI-compatible, roteamento leve/detalhado, schema, tools e eval offline), mas roda na máquina do operador: chave em arquivo local, sem fila, sem ambientes separados, sem banco vetorial e sem registro organizado de prompt, embedding e custo. O case Mundo Dev **Cloud for AI Básico + Intermediário** pede a fundação cloud do Assistente de Triagem Logística, em até 6 horas, sem treinar modelo e sem dado real de cliente.

## What Changes

- Introduzir o Assistente Cloud de Triagem: mensagem simulada entra por fila assíncrona, payload é validado, o modelo classifica e extrai, documentos internos entram por similaridade, a resposta estruturada volta ao operador e a inferência fica registrada
- Separar três ambientes (desenvolvimento, homologação, produção) com variáveis, secrets, quotas e rate limit próprios; chave de modelo nunca no código
- Consumir API de modelo gerenciada com teto de tokens, timeout e rate limit; sem fine-tuning e sem modelo próprio
- Consultar quatro tipos de documento interno fictício (regras de coleta, política de entrega, tipos de ocorrência, instruções operacionais) num banco vetorial versionado
- Organizar storage por ambiente, data, versão e tipo: datasets, embeddings, prompts versionados, artefatos de avaliação, logs e resultados de inferência, sem dado sensível
- Separar compute contínuo, função serverless (validação, limpeza de prompt, pós-processamento), cache de consulta frequente com validade, balanceador e autoscaling reproduzível
- Exigir tag, owner e observabilidade de custo (tokens, chamadas, latência, erro, compute, storage, fila) por ambiente
- Manter o menu terminal, a API de docas e os pipelines atuais; o assistente cloud é um entrypoint novo
- Premissas: provedor único **AWS**, alinhado ao cloud backend já escolhido; execução local gratuita espelha a cloud (fila, vetor, cache, storage em disco) para o aluno demonstrar sem conta paga

## Capabilities

### New Capabilities

- `cloud-ai-triage`: fluxo do assistente (fila, validação, classificação, extração, contexto, resposta estruturada, cache e erros de modelo)
- `ai-environment-controls`: ambientes dev/homolog/prod, secrets gerenciados, quotas e rate limit
- `vector-operational-docs`: documentos operacionais fictícios, embeddings por versão e busca por similaridade
- `inference-artifact-store`: layout de datasets, prompts, artefatos, logs e resultados de inferência com metadados e sem dado sensível
- `cloud-ai-platform`: compute, storage, rede, IAM mínimo, serverless, balanceador, autoscaling, tags e observabilidade de custo

### Modified Capabilities

- (nenhuma main spec arquivada; triagem local, API de docas e pipelines permanecem)

## Impact

- Novo pacote e entrypoint (ex.: `com.mundodev.filafacil.cloudai`) separado de `Main` e de `ApiMain`
- Reuso do gateway de inferência já existente; configuração de ambiente e teto de tokens passam a ser da plataforma cloud
- Storage local espelhando prefixos S3; infra nova em `infra/ai/` (IAM, fila, cache, scaling, layout de buckets) sem multi-cloud
- Redis já presente no Compose pode servir de cache; vetor local em memória ou arquivo, com pgvector/RDS documentado para a cloud
- README com o passeio do assistente e o mapa dos recursos; testes sem rede no caminho mock e um ensaio opcional com a Groq já configurada
- Referência: PDF *Cloud for AI Básico + Intermediário – FilaFácil Logística* (Escola Mundo Dev). O gráfico de pesos da página de APIs não extraiu números confiáveis; o texto do case dá maior peso à API de modelo e ao banco vetorial
