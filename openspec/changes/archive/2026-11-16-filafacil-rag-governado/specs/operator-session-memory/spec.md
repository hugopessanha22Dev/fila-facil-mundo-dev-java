# Spec Delta

## ADDED Requirements

### Requirement: Perfil e formato
O sistema MUST guardar, na sessão, o perfil operacional e a preferência de formato de resposta, junto do resumo e do último domínio.

#### Scenario: Formato pedido
- **WHEN** o operador pede resposta curta e faz outra pergunta na mesma sessão
- **THEN** a sessão ainda registra a preferência de formato curto

### Requirement: Isolamento por tenant
O sistema MUST impedir que a sessão de um tenant seja lida por outro tenant, mesmo com o mesmo identificador de sessão.

#### Scenario: Mesmo id em outro tenant
- **WHEN** o tenant B pede a sessão cujo id já existe no tenant A
- **THEN** o resumo e o domínio de A não são carregados

### Requirement: Dado sensivel recusado
O sistema MUST NOT gravar na memória um segredo ou um documento de identificação que não seja necessário para continuar a conversa.

#### Scenario: Segredo na mensagem
- **WHEN** a mensagem traz uma chave de API ou uma sequência longa de dígitos
- **THEN** esse valor não aparece no arquivo da sessão
