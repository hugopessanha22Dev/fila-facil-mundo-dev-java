# operator-session-memory Specification

## Purpose

Lembrar o resumo da sessão de um operador só enquanto o prazo valer, sem misturar sessões e sem deixar a memória criar regra que o documento não sustenta.

## Requirements

### Requirement: Memoria por sessao
O sistema MUST guardar, por identificador de sessão, o resumo da conversa, o último domínio consultado e os dados já esclarecidos nessa sessão.

#### Scenario: Segunda pergunta na mesma sessao
- **WHEN** o operador informa o domínio na primeira pergunta e faz uma pergunta curta em seguida, dentro do prazo
- **THEN** a segunda busca usa o último domínio guardado nessa sessão

### Requirement: Isolamento e expiracao
O sistema MUST impedir que uma sessão leia a memória de outra e MUST descartar a memória quando o TTL configurado vencer.

#### Scenario: Dois operadores
- **WHEN** a sessão A e a sessão B consultam domínios diferentes
- **THEN** o resumo e o último domínio de A não aparecem na busca de B

#### Scenario: Prazo vencido
- **WHEN** a pergunta seguinte chega depois do TTL
- **THEN** o último domínio da sessão não é reaplicado

### Requirement: Memoria nao substitui documento
O sistema MUST usar a memória só para escolher filtro ou continuar a conversa, e MUST NOT afirmar uma regra que esteja apenas na memória e não num chunk recuperado.

#### Scenario: Memoria sem fonte
- **WHEN** a sessão lembra uma regra que nenhum chunk vigente sustenta
- **THEN** a resposta não apresenta essa regra como evidência documental
