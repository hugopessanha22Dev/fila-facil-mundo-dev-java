# Spec Delta

## Purpose

Recuperar chunks por similaridade e por termo, dentro de um namespace, aplicando filtros e um reranking que privilegia a evidência da pergunta.

## ADDED Requirements

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
