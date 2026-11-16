# Spec Delta

## ADDED Requirements

### Requirement: Namespace de tenant
O sistema MUST separar chunks por tenant, além do domínio, e MUST NOT devolver chunk de outro tenant na busca operacional.

#### Scenario: Dois tenants
- **WHEN** a busca informa o tenant A
- **THEN** chunks do tenant B ficam fora do resultado

### Requirement: Filtro de vigencia
O sistema MUST excluir da busca operacional o documento expirado e o documento em revisão, salvo quando o acesso pedido for o de revisão.

#### Scenario: Documento expirado
- **WHEN** a busca operacional encontra um chunk cujo documento está expirado
- **THEN** esse chunk não entra no contexto

### Requirement: Expansao de consulta
O sistema MUST acrescentar termos do mesmo domínio sem remover os termos originais da pergunta.

#### Scenario: Termo expandido
- **WHEN** a pergunta usa um sinônimo cadastrado do domínio
- **THEN** a busca considera o sinônimo e a intenção original continua na consulta
