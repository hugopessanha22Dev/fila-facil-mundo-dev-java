# Spec Delta

## Purpose

Mantém uma biblioteca versionada de prompts operacionais da FilaFácil, cada um com objetivo, contexto, restrições e formato de saída estruturado para tarefas reutilizáveis de atendimento.

## ADDED Requirements

### Requirement: Estrutura padronizada do prompt
Cada prompt da biblioteca MUST declarar objetivo da tarefa, contexto FilaFácil, dados de entrada esperados, restrições explícitas (incluindo nao inventar informacoes), criterios de qualidade e formato de saida estruturado.

#### Scenario: Prompt de classificacao carregado
- **WHEN** o sistema carrega o prompt de classificacao de mensagens
- **THEN** o artefato expoe objetivo, contexto, restricoes, criterios e formato de saida definidos

### Requirement: Tarefas operacionais cobertas
A biblioteca MUST incluir prompts reutilizaveis para: classificacao de mensagens; extracao de dados; sumarizacao de ocorrencia; geracao de resposta interna; transformacao de texto livre em registro padronizado.

#### Scenario: Operador solicita sumarizacao
- **WHEN** o fluxo escolhe a tarefa de sumarizacao de ocorrencia
- **THEN** o sistema usa o prompt de sumarizacao da biblioteca (nao um texto ad hoc)

### Requirement: Saida estruturada obrigatoria
Os prompts MUST exigir saida em JSON (ou checklist controlado equivalente) com campos obrigatorios, permitindo validacao programatica posterior.

#### Scenario: Resposta sem estrutura
- **WHEN** o modelo devolve texto livre fora do formato exigido pelo prompt
- **THEN** a saida e rejeitada pela validacao e nao e aceita como resultado operacional
