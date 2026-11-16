# Proposal

## Why

A FilaFácil já tem um gateway de triagem com dois perfis, fallback, schema e um golden de três casos. O case Mundo Dev **Modelos & Inference Básico + Intermediário + Avançado** pede mais: cinco tarefas com requisitos próprios, catálogo de modelos, configuração versionada, roteamento por custo, risco, privacidade e tool use, resiliência com rate limit e circuit breaker, otimizações com critério de uso e uma regressão que bloqueia promoção. O gráfico de pesos não extraiu números.

## What Changes

- Registrar as tarefas classificação, extração, resumo, resposta assistida com tool use e busca contextual, cada uma com qualidade, custo, latência, risco, idioma, janela, tool use e privacidade
- Registrar pelo menos dois modelos com qualidade, custo, latência, janela, disponibilidade, multimodal, privacidade, tool use, idiomas e limitações
- Escolher o modelo barato na classificação simples e o robusto em extração, tool use, risco alto ou contexto longo, sem violar o mínimo de qualidade e privacidade no fallback
- Versionar temperature, top-p, max tokens, stop sequences, seed e streaming por tarefa, com rollback da versão ativa
- Recusar saída inválida de extração e de tool use, tentando de novo ou caindo no fallback antes de devolver erro ao produto
- Aplicar rate limit por provedor e por tarefa, retry com backoff, timeout por tarefa e circuit breaker aberto, half-open e fechado
- Registrar latência, custo, tokens, erro, fallback e tipo de falha por provedor e por tarefa
- Definir batching, cache, compressão de prompt e poda de contexto com quando usar, quando não usar e o risco; cache não vale para busca contextual
- Avaliar entradas ambíguas, incompletas, longas e fora de escopo fora de `inference/golden`, e bloquear a promoção quando a regressão passar do limite
- Manter o menu nas opções 0 a 12 e o total de três casos do eval antigo
- Premissa: testes usam o gateway mock, sem chamada paga. Speculative decoding fica de fora porque não há segundo fluxo de tokens local

## Capabilities

### New Capabilities

- `inference-task-catalog`: requisitos independentes das cinco tarefas de IA
- `model-catalog-selection`: atributos dos modelos e escolha justificada, inclusive no fallback
- `versioned-inference-config`: parâmetros por tarefa, streaming, versão ativa e rollback
- `resilient-inference-gateway`: rate limit, backoff, timeout, circuit breaker, validação com retry e métricas
- `inference-cost-controls`: batching, cache, compressão e poda com critério de uso
- `inference-regression-gate`: casos adversariais e bloqueio de promoção por regressão

### Modified Capabilities

- (nenhuma spec principal de inferência está arquivada; o gateway de triagem permanece e passa a ser chamado por esta camada)

## Impact

- Extensão do pacote `com.mundodev.filafacil.inference`, sem opção nova de menu
- Catálogo e versões em `src/main/resources/inference/catalog/`, fora de `inference/golden`
- Casos adversariais em outro diretório de recursos
- README com as cinco tarefas, o circuit breaker e o motivo de não cachear a busca contextual
- Referência: PDF *Modelos & Inference Básico + Intermediário + Avançado – FilaFácil Logística* (Escola Mundo Dev)
