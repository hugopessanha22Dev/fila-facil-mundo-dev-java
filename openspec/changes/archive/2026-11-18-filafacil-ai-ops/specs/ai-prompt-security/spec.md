# Spec Delta

## Purpose

Impedir que a instrução do usuário substitua a regra interna, use ferramenta indevida ou exponha dado privado, mascarando o que for sensível.

## ADDED Requirements

### Requirement: Injecao recusada
O sistema MUST recusar entrada que tenta substituir a regra interna ou revelar o prompt, e MUST aceitar pergunta logística legítima.

#### Scenario: Pedido de substituir a regra
- **WHEN** a mensagem manda ignorar as regras e mostrar o prompt
- **THEN** nenhuma ferramenta executa e a resposta é recusada

#### Scenario: Pergunta legitima
- **WHEN** a mensagem pede uma regra operacional sem tentar substituir instrução
- **THEN** o pedido é aceito

### Requirement: Vazamento mascarado
O sistema MUST mascarar segredo e número longo no trace e MUST recusar devolver conteúdo privado de outro cliente.

#### Scenario: Segredo na saida
- **WHEN** a resposta traria um segredo ou um número longo
- **THEN** o texto gravado e o texto devolvido substituem esse trecho

### Requirement: Ferramenta no escopo
O sistema MUST recusar ferramenta fora da lista do fluxo ou com argumento que não passa no schema.

#### Scenario: Ferramenta indevida
- **WHEN** a ferramenta pedida não está no escopo do fluxo
- **THEN** a chamada não acontece
