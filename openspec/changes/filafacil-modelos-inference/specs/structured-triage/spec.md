# Spec Delta

## Purpose

Define o contrato JSON da triagem operacional e exige validação programática da saída do modelo antes de qualquer uso interno no FilaFácil.

## ADDED Requirements

### Requirement: Entradas operacionais suportadas
O sistema MUST aceitar texto livre classificado em pelo menos: consulta de entrega, ocorrência operacional e reclamação de cliente.

#### Scenario: Mensagem de reclamação
- **WHEN** o operador envia uma mensagem descrevendo entrega não concluída e pede resolução
- **THEN** a triagem produz saída com tipo de solicitação compatível com reclamação

### Requirement: Schema JSON obrigatório
Toda resposta de inferência aceita MUST conformar a um JSON schema do projeto incluindo: tipo da solicitação; código de coleta/entrega quando existir; cliente quando existir; sentimento/tom; prioridade sugerida; resumo operacional; próxima ação; necessidade de intervenção humana com justificativa; nível de confiança; campos ausentes ou com erro; indicadores de falha que bloqueiam uso sem revisão.

#### Scenario: Resposta completa válida
- **WHEN** o modelo retorna JSON que satisfaz todos os campos obrigatórios do schema
- **THEN** o sistema marca a inferência como bem-sucedida e disponibiliza a saída estruturada

### Requirement: Rejeição sem validação
O sistema MUST NOT aceitar resposta livre ou JSON inválido/incompleto como resultado de triagem utilizável; falhas de schema MUST gerar erro tipado e exigir revisão humana ou nova tentativa.

#### Scenario: JSON fora do schema
- **WHEN** o modelo retorna texto não-JSON ou JSON que viola o schema
- **THEN** a saída é rejeitada, o erro é registrado e nenhum resultado estruturado é aplicado internamente

### Requirement: Exposição no terminal
O menu interativo MUST oferecer uma opção para colar/digitar a mensagem e exibir o JSON estruturado validado (ou a mensagem de rejeição/erro) sem encerrar a aplicação.

#### Scenario: Operador usa opção de triagem
- **WHEN** o operador escolhe a opção de triagem e informa uma mensagem
- **THEN** o terminal mostra o resultado estruturado validado ou o erro de validação/gateway
