# Proposal

## Why

A FilaFácil já registra ocorrência no agente e no cadastro de coletas, mas cada área ainda decide sozinha o que fazer com cliente ausente, endereço divergente, pacote danificado, entrega fora da janela, rota bloqueada e reagendamento. O case Mundo Dev **Design de Software Avançado** pede um orquestrador local desse ciclo: abrir, analisar, escalar, resolver e consultar, com domínio rico, contextos delimitados e adaptadores em memória. O gráfico de pesos não extraiu números; o texto dá maior peso ao domínio com DDD e aos casos de uso.

## What Changes

- Modelar a exceção de entrega como entidade com identidade, tipo, origem, severidade, status, descrição, abertura e janela de resolução
- Colocar a transição de estado na entidade e nos value objects, com eventos de domínio a cada mudança relevante
- Expor casos de uso de abrir, analisar, escalar, resolver, cancelar e consultar, dependendo só de portas
- Implementar repositório, publicador de eventos e gateway dos contextos vizinhos em memória
- Documentar os cinco bounded contexts, o sentido upstream/downstream, a linguagem publicada e a camada anticorrupção
- Registrar a decisão arquitetural: por que a regra ficou na entidade e por que os termos vizinhos não entram crus no domínio
- Manter o menu nas opções 0 a 12 e não alterar coletas, agente nem RAG
- Premissa: a janela de resolução é informada na abertura. Severidade alta ou crítica exige escala antes da resolução. O PDF não fixa horas nem nomes de classe

## Capabilities

### New Capabilities

- `delivery-exception-domain`: entidade, value objects, regras de transição e eventos de domínio
- `exception-application-ports`: casos de uso de abrir, analisar, escalar, resolver, cancelar e consultar
- `exception-memory-adapters`: repositório, publicador e gateway em memória, acionados por CLI
- `exception-context-map`: cinco contextos, linguagem publicada e tradução anticorrupção
- `exception-architecture-decision`: decisão que compara o modelo anêmico e o vocabulário único com o desenho adotado

### Modified Capabilities

- (nenhuma; as specs já arquivadas permanecem)

## Impact

- Novo pacote hexagonal (ex.: `com.mundodev.filafacil.excecoes`), com o domínio sem import de arquivo, Jackson, Javalin ou banco
- CLI nova (ex.: `exec:java@excecoes`), sem opção nova no menu
- Documentos em `docs/excecoes/` para o mapa de contextos e o ADR
- Testes no domínio e nos casos de uso com adaptadores em memória
- Referência: PDF *Design de Software Avançado – FilaFácil Logística* (Escola Mundo Dev). Este PDF não tinha change anterior neste repositório
