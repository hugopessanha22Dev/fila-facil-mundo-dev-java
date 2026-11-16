# Design

## Context

O pacote `com.mundodev.filafacil.inference.prompt` já carrega cinco prompts, compõe objetivo, restrições, formato e few-shot num texto único, guarda a sessão em memória e rejeita status inventado. `PromptEngineeringTest` exige esses cinco ids, os quatro tipos de exemplo e a conclusão conversacional com `proximaAcao`. O menu já usa a opção 7 para esse atendimento e permanece nas opções 0 a 12. A memória de domínio do RAG continua em `operator-session-memory` e a frase de ausência do RAG não muda. Ver proposal.md para o motivo.

## Goals / Non-Goals

**Goals:**

- Três camadas e delimitadores sem apagar o texto que os testes atuais procuram no prompt composto
- Priorização em quatro etapas, com conclusão factual bloqueada sem evidência
- Cobertura de exemplos por fluxo, inclusive um contraexemplo em cada prompt
- Retomada que leva intenção, pendências e evidências, e deixa de fora o trecho irrelevante
- Cadeia de validação com autocorreção só de formato
- Resposta calibrada para prazo, status, responsável, política e código sem fonte

**Non-Goals:**

- Opção nova de menu
- Arquivo novo em `inference/golden`
- Chamada paga dentro de `mvn test`
- Alterar a frase `Nao encontrado nos documentos.`
- Reescrever o schema `justificativaIntervencencao`
- Fazer a memória de prompt cumprir o papel do TTL e do domínio do RAG

## Decisions

1. O sexto prompt entra na mesma biblioteca, com id `priorizar`. O teste que hoje exige tamanho 5 passa a exigir os cinco ids antigos mais esse. Os cinco JSON atuais permanecem.
2. As regras globais ficam em `src/main/resources/inference/prompts/camadas.json`. O compositor emite os blocos `Regras globais`, `Regras da tarefa` e `Regras de formato`, e coloca a mensagem entre `<<<ENTRADA>>>` e `<<<FIM>>>`. A tarefa e o formato continuam vindo da definição já carregada. Os marcadores `Exemplos few-shot` e `Memoria da sessao` permanecem, porque os testes atuais dependem deles.
3. A priorização é um serviço à parte do `handleTurn` atual. As etapas saem numa lista ordenada: `intencao`, `entidades`, `evidencias`, `resposta`. Sem evidência, a resposta vem calibrada e a prioridade não sai como fato. A opção 7 não ganha ramo novo.
4. O catálogo few-shot ganha exemplos fictícios até cada id, inclusive `priorizar`, ter três positivos e um contraexemplo. Os tipos `negativo` e `escalacao` que já existem ficam no arquivo.
5. A sessão de prompt grava JSON num diretório injetável. O teste usa pasta temporária. A retomada monta o resumo só com intenção, campos pendentes e ids de evidência. Um marcador de histórico fora desse resumo não entra no prompt. Evidência com origem `memoria` não conta como documento. A sessão do RAG não é lida nem escrita.
6. A cadeia nova fica ao lado do validador atual, sem mudar o contrato que lança exceção para status inventado. A autocorreção só remove cerca de código quando o miolo é JSON. Campo obrigatório ausente não é preenchido. Depois do retry já existente, o resultado bloqueado traz revisão humana e não devolve o texto inválido como saída aceita.
7. O calibrador olha prazo, status, responsável, política e código. A fonte pode ser a mensagem, um campo de sessão com origem `fato` ou uma evidência de ferramenta cujo nome esteja na lista já permitida. Qualquer outro valor vira incerteza e deixa de ser fato.

## Risks / Trade-offs

- [O teste da biblioteca quebra se o sexto prompt entrar sem atualizar a contagem] → a tarefa muda a asserção junto com o recurso.
- [Delimitar a entrada pode esconder o texto que o mock usa para extrair código] → o delimitador cerca a mensagem, mas o texto interno continua visível para o mock.
- [A cadeia nova poderia engolir a conclusão da opção 7] → ela não entra em `handleTurn`. O fluxo de dois turnos segue com `proximaAcao`.
- [Retomada em arquivo poderia gravar coleta real] → o diretório é injetável e o teste não aponta para `data/coletas.json`.

## Migration Plan

- Acrescentar o prompt, as camadas, os exemplos, a sessão de prompt e a seção no README.
- Rollback: remover esses recursos e a seção. Os cinco prompts, os três golden e a opção 7 permanecem.

## Open Questions

- Nenhuma que mude o escopo. A lista de ferramentas autorizadas reusa os nomes já permitidos no mock operacional, sem incluir ferramenta nova.
