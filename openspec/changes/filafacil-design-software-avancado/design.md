# Design

## Context

O case de Design de Software Avançado pede um orquestrador de exceções de entrega: cliente ausente, endereço divergente, pacote danificado, fora da janela, rota bloqueada e reagendamento. O repositório já tem Factory, Strategy e Observer no pacote `coletas`, e um agente que consulta e registra ocorrência. Nenhum dos dois isola um domínio de exceção com value objects, portas e mapa de contextos. O menu permanece nas opções 0 a 12. O gráfico de pesos do PDF não extraiu números; o texto dá maior peso ao domínio e aos casos de uso. O projeto compila em Java 17.

## Goals / Non-Goals

**Goals:**

- Domínio de exceção executável, com transições testadas e eventos de domínio
- Casos de uso que só enxergam portas de repositório, publicação e gateway
- Adaptadores em memória e uma CLI para o ciclo completo
- Mapa dos cinco contextos e tradução dos termos vizinhos
- ADR que compara as alternativas rejeitadas com o desenho adotado

**Non-Goals:**

- Banco, API HTTP obrigatória, fila real, frontend, autenticação ou deploy
- Microsserviço por contexto
- Nova opção de menu
- Alterar `Coleta`, o agente ou o RAG
- Inventar os números do gráfico de pesos

## Decisions

1. Pacote `com.mundodev.filafacil.excecoes`, separado em `domain`, `application` e `adapters`. O domínio não importa Jackson, Javalin, `java.nio.file` nem JDBC. Um teste falha se algum arquivo do domínio contiver esses imports.
2. A entidade `ExcecaoEntrega` guarda identificador, código da entrega, tipo, origem, severidade, status, descrição, abertura e fim da janela. Tipo, origem, severidade e status são value objects fechados. Tipos: `cliente_ausente`, `endereco_divergente`, `pacote_danificado`, `fora_da_janela`, `rota_bloqueada`, `reagendamento`. Origens: `atendimento`, `operacao`, `financeiro`, `roteirizacao`. Severidades: `baixa`, `media`, `alta`, `critica`. Status: `aberta`, `em_analise`, `escalada`, `resolvida`, `cancelada`.
3. A transição mora na entidade. Analisar sai de `aberta`. Escalar sai de `em_analise` e só é obrigatória antes de resolver quando a severidade é `alta` ou `critica`; severidade baixa ou média pode resolver a partir de `em_analise`. Resolver também exige instante dentro da janela recebida na abertura. Cancelar sai de `aberta` ou `em_analise`. Cada transição aceita devolve um evento com status anterior e novo. Essa escala por severidade é premissa: o PDF pede o ato de escalar e não fixa o critério.
4. Casos de uso `Abrir`, `Analisar`, `Escalar`, `Resolver`, `Cancelar` e `Consultar` recebem portas no construtor. Abrir recusa outra exceção ainda aberta da mesma entrega e do mesmo tipo. Publicar o evento é responsabilidade do caso de uso, chamando a porta; a regra de qual transição existe continua na entidade.
5. Adaptadores `RepositorioMemoria`, `PublicadorMemoria` e `GatewayContextos`. O gateway traduz `route_blocked`, `customer_absent` e `damaged_charge` para tipo e origem. Termo fora dessa tabela não abre exceção. A CLI `exec:java@excecoes` usa esses adaptadores na mesma execução: `abrir`, `analisar`, `escalar`, `resolver`, `cancelar`, `consultar`. O estado não sobrevive ao fim do processo. Recusa imprime o motivo e devolve código 2, sem stack trace de regra de negócio.
6. `docs/excecoes/context-map.md` nomeia os cinco contextos. Gestão de exceções é o núcleo. Roteirização, atendimento e financeiro são upstream e entram pela linguagem publicada do gateway. Auditoria é downstream e lê os eventos já publicados, com vocabulário de trilha, sem importar a entidade. `docs/excecoes/adr-0001-orquestrador.md` compara comando anêmico com entidade rica, e vocabulário único com contextos traduzidos. A versão inicial descrita é a transição dentro da CLI; a versão adotada move essa transição para a entidade e mantém os mesmos seis casos de uso.

## Risks / Trade-offs

- [Severidade alta ou crítica como critério de escala não está numérica no PDF] → fica explícita na proposta e no ADR, e o teste cobre os dois lados.
- [Estado só em memória some ao fechar a CLI] → o PDF aceita adaptador em memória; o README diz que cada comando da demonstração compõe uma execução.
- [Auditoria sem processo próprio pode parecer só documento] → o publicador guarda os eventos e o mapa diz que a auditoria consome essa lista, sem segundo banco.
- [O agente já registra ocorrência] → este orquestrador não o chama e não reaproveita o store dele.

## Migration Plan

- Acrescentar o pacote, a execução `excecoes` no `pom.xml`, os dois documentos e a seção no README.
- Rollback: remover o pacote, a execução, os documentos e a seção. `data/coletas.json` e o menu não mudam.

## Open Questions

- Nenhuma que mude o escopo. A janela vem na abertura. Fora dela, a resolução é recusada. Cancelar não gera resolução.
