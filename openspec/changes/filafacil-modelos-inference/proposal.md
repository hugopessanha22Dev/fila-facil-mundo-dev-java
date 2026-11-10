# Proposal

## Why

O FilaFácil já cobre coletas em terminal (JSON, ViaCEP, estado e testes), mas a triagem operacional de mensagens (consultas, ocorrências, reclamações) ainda é 100% manual, sem inferência controlada nem validação de saída. O case Mundo Dev **Modelos & Inference Básico + Intermediário** pede uma camada de IA com roteamento entre modelos, JSON schema, tools, fallback e avaliação offline — evolução natural sobre a base atual, sem web nem banco.

## What Changes

- Introduzir camada de serviço de inferência (entrada → execução → validação → resposta); nenhuma chamada a modelo espalhada pelo resto do código
- Processar solicitações operacionais (consulta de entrega, ocorrência, reclamação) e devolver JSON estruturado validado por schema do projeto
- Comparar e rotear entre pelo menos dois perfis de modelo (triagem leve vs análise detalhada), com parâmetros de inferência por cenário
- Implementar robustez: autenticação via env, timeouts, retries limitados, fallback de modelo, logs básicos e estimativa de custo por chamada
- Definir tools com contrato (entrada/saída/erro), inicialmente mockadas: status de entrega, buscar coleta, registrar ocorrência, calcular prioridade, encaminhar humano
- Criar golden dataset + avaliação offline (JSON válido, campos, latência, custo, fallback) e análise de falhas no README
- Expor no menu terminal uma opção de “triagem por mensagem” (texto livre → resultado estruturado); manter CRUD existente intacto
- Premissa de provedor (ajustável no apply): API OpenAI-compatible (ex.: OpenAI ou Groq) com dois `modelId` distintos + modo mock para testes sem rede/chave

## Capabilities

### New Capabilities

- `inference-gateway`: camada única de chamada a modelos (auth, timeout, retry, fallback, log, custo, parâmetros por cenário)
- `structured-triage`: contrato JSON schema da triagem operacional e validação programática antes de aceitar a resposta
- `model-routing`: escolha automática entre perfil leve e detalhado conforme tipo/criticidade/tamanho da entrada
- `inference-tools`: contratos de tools simuladas usadas na inferência (status, coleta, ocorrência, prioridade, humano)
- `offline-inference-eval`: golden dataset, métricas offline e registro de falhas para ajuste

### Modified Capabilities

- (nenhuma — `openspec/specs/` sem main specs arquivadas; o menu/CRUD do Módulo 2 permanece e é estendido pela opção de triagem)

## Impact

- Novos pacotes sob `com.mundodev.filafacil.inference` (gateway, routing, schema, tools, eval)
- `MenuController` / `Main`: nova opção de menu; wiring do gateway
- `pom.xml`: possível lib mínima de JSON Schema (ou validação manual com Jackson); HttpClient do JDK
- Secrets: chave de API só via variável de ambiente (nunca commitada)
- Continua terminal + JSON local; sem web, sem MySQL, sem framework pesado de app
- Referência: PDF *Modelos & Inference Básico + Intermediário – FilaFácil Logística* (Escola Mundo Dev)
