# vector-retrieval Specification

## Purpose

Recuperar chunks por similaridade e por termo, dentro de um namespace, aplicando filtros e um reranking que privilegia a evidência da pergunta.

## Requirements

### Requirement: Embedding por chunk
O sistema MUST gerar um embedding para cada chunk e guardá-lo com os metadados do chunk.

#### Scenario: Chunk consultavel
- **WHEN** a ingestão termina
- **THEN** cada chunk ativo possui vetor e pode ser comparado com a pergunta

### Requirement: Namespace e filtros
O sistema MUST separar chunks por domínio e MUST filtrar por tipo, versão, nível de acesso e domínio antes de montar o contexto.

#### Scenario: Acesso de parceiro
- **WHEN** a pergunta pede nível de acesso parceiro
- **THEN** chunks restritos a operador ficam fora do resultado

#### Scenario: Dominio explicito
- **WHEN** a pergunta restringe o domínio a entregas
- **THEN** chunks de outros domínios não entram no top-k

### Requirement: Top-k hibrido e reranking
O sistema MUST devolver no máximo o top-k configurado, MUST combinar similaridade e termo quando a busca híbrida estiver ligada, e MUST reranquear para subir o chunk cuja seção combina com a pergunta.

#### Scenario: Pergunta ambigua
- **WHEN** a pergunta casa com dois chunks e só um contém o termo da seção pedida
- **THEN** o reranking coloca esse chunk à frente do outro

#### Scenario: Hibrido encontra o termo
- **WHEN** a busca híbrida está ligada e a pergunta repete um termo que o vetor sozinho ranqueia abaixo do corte
- **THEN** o chunk que contém o termo entra no resultado

### Requirement: Parametros de recuperacao
O sistema MUST ler top-k, chunk size, overlap, peso do híbrido e o interruptor de reranking de uma configuração, sem recompilar o programa para mudá-los.

#### Scenario: Top-k menor
- **WHEN** a configuração reduz o top-k de 4 para 1
- **THEN** a busca devolve no máximo um chunk

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
