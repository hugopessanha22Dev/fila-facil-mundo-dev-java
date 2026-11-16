# Tasks

## 1. Corpus e ciclo de vida

- [x] 1.1 Histórico operacional: teste de que o corpus padrão tem oito famílias, inclusive `historico`, e de que um domínio fora da lista continua rejeitado
- [x] 1.2 Status, hash e linhagem: teste de que a versão substituída vai para o namespace histórico, a busca operacional devolve a vigente, o hash não muda quando o texto não muda, e a execução registra só o documento republicado

## 2. Busca governada

- [x] 2.1 Tenant e vigência: teste de que o tenant B não recebe chunk do tenant A, de que documento expirado fica fora da busca operacional, e de que em revisão só aparece para o acesso `revisor`
- [x] 2.2 Expansão: teste de que um sinônimo do domínio entra na consulta e os termos originais permanecem

## 3. Contexto da resposta

- [x] 3.1 Compressão: teste de que um parágrafo repetido sai do contexto e de que o chunk com o termo distintivo da pergunta permanece
- [x] 3.2 Top-k: teste de que a pergunta de um domínio não passa do top-k configurado e de que a pergunta de dois domínios pode reunir os dois sem passar de `maxTopK`

## 4. Memória

- [x] 4.1 Perfil, tenant e segredo: teste de que a preferência de formato curto sobrevive à pergunta seguinte, de que o mesmo id no tenant B não carrega o resumo de A, e de que chave `sk-` ou sequência de 11 dígitos não entra no arquivo da sessão

## 5. Avaliação

- [x] 5.1 Falhas novas em `rag/falhas-governanca.json`: teste de que documento expirado, top-k insuficiente, expansão inadequada e reranking fraco aparecem no relatório com as ações bloquear na busca ativa, ampliar o teto, preservar a intenção e religar o reranking

## 6. Compatibilidade

- [x] 6.1 README com status do documento, namespace histórico e o motivo de não guardar dado sensível; menu segue na opção 12 e sem opção 13; a ausência continua `Nao encontrado nos documentos.`; `mvn test` verde com o total do eval antigo ainda em 3

## Workflow follow-up

- Arquivar o change depois da revisão
