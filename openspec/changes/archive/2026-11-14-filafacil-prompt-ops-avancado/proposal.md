# Proposal

## Why

A FilaFácil já tem biblioteca de cinco prompts, few-shot, sessão de um atendimento e uma rubrica que rejeita status inventado. O case avançado de Prompt Engineering pede o que ainda não está fechado: camadas de instrução, delimitação da entrada, raciocínio de priorização em etapas, exemplos suficientes por fluxo, retomada seletiva de sessão, pipeline de validação com autocorreção de formato e resposta calibrada quando falta evidência. O gráfico de pesos não extraiu números.

## What Changes

- Separar o prompt em três camadas: regras globais, regras da tarefa e regras de formato, com a entrada do operador delimitada fora da instrução
- Acrescentar o fluxo de priorização guiada, em quatro etapas: intenção, entidades, evidências e resposta, sem concluir quando não houver base
- Completar o catálogo para pelo menos três exemplos positivos por fluxo e um contraexemplo em cada prompt, sem dados reais
- Retomar um atendimento anterior carregando só intenção, campos pendentes e evidências usadas, sem despejar o histórico inteiro no prompt
- Registrar as evidências de cada resposta e impedir que a memória da sessão substitua fonte documental ou resultado de ferramenta autorizada
- Validar a saída em cadeia: schema, parser, campos obrigatórios, tipos, consistência, rubrica, retry, autocorreção só de formato e, se continuar inválida, bloqueio com revisão humana
- Quando faltar base para prazo, status, responsável, política ou código, devolver incerteza ou pedido de clarificação em vez do dado inventado
- Manter o menu nas opções 0 a 12. A opção 7 de atendimento conversacional permanece o único ponto de menu
- Premissa: testes no mock, sem chamada paga. Os três JSON de `inference/golden` não mudam

## Capabilities

### New Capabilities

- `hierarchical-prompt-composition`: três camadas de instrução e delimitação da entrada
- `guided-prioritization`: raciocínio em quatro etapas, com conclusão bloqueada sem evidência
- `prompt-example-coverage`: três exemplos positivos por fluxo e um contraexemplo por prompt
- `prompt-session-resume`: retomada seletiva do estado e registro de evidências que não viram documento
- `response-validation-pipeline`: cadeia de validação, autocorreção de formato e revisão humana
- `calibrated-operational-answer`: resposta incerta quando prazo, status, responsável, política ou código não têm fonte

### Modified Capabilities

- (nenhuma spec principal de prompt está arquivada; a memória RAG de `operator-session-memory` permanece dona do domínio e do TTL e não muda de requisito)

## Impact

- Extensão de `com.mundodev.filafacil.inference.prompt`, sem opção nova de menu
- Novo prompt e exemplos em `src/main/resources/inference/prompts/` e `inference/fewshot/`, fora de `inference/golden`
- Sessões de prompt em diretório próprio de teste, sem gravar `data/coletas.json` e sem alterar a frase de ausência do RAG
- README com as três camadas, as quatro etapas e o motivo da resposta calibrada
- Referência: PDF *Prompt Engineering & Interaction Básico + Intermediário + Avançado – FilaFácil Logística* (Escola Mundo Dev)
