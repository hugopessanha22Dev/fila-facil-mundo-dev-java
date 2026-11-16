# Tasks

## 1. Camadas e exemplos

- [x] 1.1 Camadas: teste de que o prompt composto tem blocos separados de regras globais, da tarefa e de formato, e de que uma frase imperativa do operador aparece só entre `<<<ENTRADA>>>` e `<<<FIM>>>`, sem apagar `Exemplos few-shot` nem `Memoria da sessao`
- [x] 1.2 Exemplos: teste de que cada fluxo, inclusive `priorizar`, tem ao menos três positivos e um contraexemplo, e de que os tipos negativo e escalação já existentes continuam no catálogo

## 2. Priorização e retomada

- [x] 2.1 Priorização: teste das quatro etapas `intencao`, `entidades`, `evidencias` e `resposta` nessa ordem, e de que sem evidência a prioridade não sai como fato; a biblioteca passa a contar `priorizar` junto dos cinco ids antigos
- [x] 2.2 Retomada: teste, em diretório temporário, de que o prompt remontado traz a intenção guardada e omite o trecho irrelevante, e de que um prazo que existe só na memória não vira evidência

## 3. Validação e resposta calibrada

- [x] 3.1 Cadeia: teste de que um JSON dentro de cerca de código é aceito depois da correção de formato, de que campo obrigatório ausente não é inventado, e de que o retry esgotado bloqueia para revisão humana sem devolver o texto inválido
- [x] 3.2 Calibração: teste de que prazo e código ausentes da mensagem, da sessão com origem fato e da evidência de ferramenta autorizada não saem como fato, e a saída marca incerteza

## 4. Compatibilidade

- [x] 4.1 README com as três camadas, as quatro etapas e o motivo da resposta calibrada; menu segue na opção 7 e sem opção 13; o fluxo conversacional de dois turnos continua com `proximaAcao`; `mvn test` verde com o total do eval antigo ainda em 3

## Workflow follow-up

- Smoke com Groq fica fora do `mvn test`
- Arquivar o change depois da revisão
