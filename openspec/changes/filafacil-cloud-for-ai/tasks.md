# Tasks

## 1. Ambientes, quotas e secrets

- [x] 1.1 Criar `FILAFACIL_AI_ENV` (`dev`, `homolog`, `prod`) e `cloud-ai-quotas.json` com timeout, teto de tokens, rate limit, quota e limiar de alerta por ambiente; verificar teste que homolog e prod nao herdam a credencial nem a quota permissiva de dev
- [x] 1.2 Implementar leitura de chave so por variavel de ambiente, com recusa em prod se a chave estiver vazia ou apontar para arquivo dentro do repositorio; verificar teste de mascaramento e de subida recusada, e que nenhum teste commita segredo

## 2. Fila e funcoes serverless

- [x] 2.1 Implementar fila por ambiente com estados aceita, em processamento, concluida e falha, mais identificador na publicacao; verificar teste de mensagem valida enfileirada e de payload sem texto rejeitado sem chamada de modelo
- [x] 2.2 Extrair validacao de payload, limpeza de prompt e normalizacao da resposta em operacoes chamadas pela orquestracao, nao embutidas nela; verificar teste unitario de cada operacao e que prompt persistido sai limpo

## 3. Documentos e banco vetorial

- [x] 3.1 Adicionar fixtures ficticias das quatro familias (regras de coleta, politica de entrega, tipos de ocorrencia, instrucoes operacionais) e recusar familia desconhecida; verificar teste de carga com um documento de cada familia e de rejeicao
- [x] 3.2 Implementar indice local versionado por ambiente e dominio, busca por cosseno com corte, e reprocessamento que nao mistura versao nem ambiente; verificar teste de hit com familia e versao, de contexto ausente abaixo do corte, e de versao antiga ainda enderecavel

## 4. Artefatos de inferencia

- [x] 4.1 Gravar datasets bruto e tratado, prompts com id de versao imutavel, artefato de avaliacao por modelo/data/workload e resultado de inferencia (id, modelo, versao de prompt, latencia, tokens, custo, status, erro); verificar arquivos no layout `data/cloud-ai/{env}/...` e referencia da versao de prompt no resultado
- [x] 4.2 Mascarar chave e credencial antes de log e de resultado; verificar teste com amostra `gsk_`, `sk-` e `AQ.` gravada ja mascarada

## 5. Orquestracao, cache e CLI

- [x] 5.1 Orquestrar drain: validar, buscar contexto, chamar o gateway existente sob timeout, teto e rate limit do ambiente, devolver resposta estruturada e registrar falha de timeout ou limite sem repetir alem do teto; verificar teste com gateway fake para sucesso, timeout e rate limit
- [x] 5.2 Cachear pela chave ambiente + versao de prompt + versao de documentos + texto, com TTL; usar Redis se estiver no ar e memoria no teste; verificar hit sem nova chamada e miss quando a versao ou o prazo muda
- [x] 5.3 Expor `CloudAiMain` via `exec:java@cloud-ai` (`enqueue` e `drain`) e imprimir resumo de tokens, chamadas, latencia, erro, storage, profundidade da fila e custo, com alerta acima do limiar; verificar execucao local com gateway fake e que o menu 0–12 nao ganha opcao nova

## 6. Infra AWS e documentacao

- [x] 6.1 Adicionar `infra/ai/` com fila, Lambda, ALB, auto scaling min/max e IAM minimo por ambiente (secret, prefixos, logs, fila), mais validador que falha se faltar tag `Environment`, `Owner` ou `Purpose`; verificar o validador num fixture invalido e num valido
- [x] 6.2 Documentar no README o assistente, os tres ambientes, o layout de artefatos, o comando local e o mapa AWS, deixando explicito que nao ha treino, fine-tuning, dado real nem segundo provedor; verificar que os comandos do README batem com o entrypoint

## Workflow follow-up

- Ensaio opcional com a Groq ja configurada, fora do `mvn test`.
- Arquivar o change (`/opsx-archive`) depois da revisao.
