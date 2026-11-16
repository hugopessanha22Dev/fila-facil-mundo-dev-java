# Tasks

## 1. Catálogo e seleção

- [x] 1.1 Tarefas: teste de que classificação é risco baixo, pt-BR, sem tool use e janela curta; extração é risco alto, JSON e sem streaming; busca contextual tem cache proibido
- [x] 1.2 Modelos: teste dos atributos do modelo barato e do robusto, escolha barata na classificação, escolha robusta com tool use, e fallback recusado quando a privacidade do reserva é insuficiente

## 2. Configuração e gateway

- [x] 2.1 Versões: teste de temperature da extração menor ou igual à da classificação, streaming desligado na extração, promoção da v2 e rollback que restaura a anterior sem apagar a v2
- [x] 2.2 Gateway: teste de execução marcada como `gateway-central`, rate limit que recusa antes do mock, circuito que abre e não consulta o mock, e JSON inválido da extração que gera segunda tentativa antes de aceitar o texto
- [x] 2.3 Métricas da execução com latência, custo, tokens, erro, fallback e tipo de falha, separáveis por tarefa

## 3. Otimização, avaliação e menu

- [x] 3.1 Política das quatro estratégias com uso, proibição e risco; teste de classificação repetida servida do cache, busca contextual repetida sem cache, e poda que conserva `evidencia-critica`
- [x] 3.2 Casos ambíguo, incompleto, longo e fora de escopo em `src/main/resources/inference/adversarial/`, sem arquivo novo em `inference/golden`
- [x] 3.3 Regressão: candidata acima do limite não promove; candidata dentro do limite promove
- [x] 3.4 README com as cinco tarefas, o circuit breaker e o motivo de não cachear a busca contextual; menu segue na opção 12 e sem opção 13; `mvn test` verde com o total do eval antigo ainda em 3

## Workflow follow-up

- Smoke com Groq fica fora do `mvn test`
- Arquivar o change depois da revisão
