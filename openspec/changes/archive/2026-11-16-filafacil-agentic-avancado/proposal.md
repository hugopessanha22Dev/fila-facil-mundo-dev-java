# Proposal

## Why

O agente da FilaFácil já consulta, registra, cita regra e para nos nove cenários atuais. O case avançado pede controle que ainda não existe: tempo máximo, permissão por ferramenta, plano barrado antes da chamada, aprovação humana para ação de alto impacto, erro temporário que se resolve no retry e erro permanente que escala. O gráfico de pesos não extraiu números.

## What Changes

- Impor um tempo máximo de execução, além dos tetos de passos, tentativas e chamadas que já valem
- Registrar permissão de cada ferramenta antes da chamada
- Oferecer consulta de entrega, publicação em fila, histórico do caso e portal interno simulado, sem trocar a consulta que os fluxos atuais já usam
- Distinguir falha temporária, que acerta na tentativa seguinte, de falha permanente, que escala para humano
- Barrar plano inseguro antes de qualquer chamada e segurar ação de alto impacto até haver aprovação
- Gravar as etapas do fluxo e um resumo com evidências, ações e pendências
- Acrescentar quatro cenários e as métricas de retry, robustez, segurança e qualidade da trajetória
- Manter os nove cenários atuais, o menu nas opções 0 a 12 e `data/coletas.json` intacto
- Premissa: simulação local, sem modelo pago e sem navegador real

## Capabilities

### New Capabilities

- (nenhuma; o comportamento novo cabe nas specs do agente já arquivadas)

### Modified Capabilities

- `agent-charter`: tempo máximo e permissão por ferramenta
- `agent-tool-contracts`: consulta de entrega, fila, histórico, portal e erro temporário ou permanente
- `agent-step-loop`: etapas nomeadas e resumo com evidências, ações e pendências
- `agent-guardrails`: plano inseguro barrado antes da chamada e ação sensível aguardando aprovação
- `agent-scenario-eval`: quatro cenários novos e métricas de trajetória

## Impact

- Extensão de `com.mundodev.filafacil.agent` e de `src/main/resources/agent/`
- O teste que exige nove cenários passa a exigir treze, no mesmo passo dos cenários novos
- A primeira consulta dos fluxos atuais continua sendo `consultar_coleta`
- `run(String)` não zera os flags de falha
- README com tempo máximo, aprovação e o motivo de barrar o plano antes da chamada
- Referência: PDF *Agentic Systems Básico + Intermediário + Avançado – FilaFácil Logística* (Escola Mundo Dev)
