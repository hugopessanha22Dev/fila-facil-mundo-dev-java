# Proposal

## Why

A FilaFácil já classifica mensagens com IA e já guarda prompt, embedding e ambiente no assistente cloud, mas uma troca de prompt, dataset, embedding ou configuração de modelo ainda pode seguir sem avaliação offline nem bloqueio de release. O case Mundo Dev **DevOps for AI Básico + Intermediário** pede a esteira que só promove o que passou em qualidade, em até 6 horas, sem treinar modelo e sem dado real de cliente.

## What Changes

- Criar a esteira AI Delivery: lint, testes, validação de prompt, schema de saída, avaliação offline em golden dataset, checagem de regressão, build e promoção simulada para homologação e produção
- Bloquear a promoção quando um gate mínimo falhar; nenhuma versão chega ao ambiente final sem avaliação offline aprovada
- Versionar prompt, golden dataset, embedding, schema e configuração de modelo, e gravar a combinação usada em cada build, avaliação e deploy
- Permitir rollback para a combinação anterior de prompt, configuração, embedding ou aplicação
- Registrar trace de inferência com versão, modelo, latência, custo, tokens, retries, fallback e erro, sem segredo e sem dado sensível
- Aplicar guardrails de entrada e de saída, retry com teto, fallback de modelo e encaminhamento humano quando a saída não cumprir o schema
- Separar gates por tipo de artefato, para uma oscilação irrelevante de latência no mock não barrar a release
- Manter o CI de manifesto, o menu e o assistente cloud atuais; a esteira é um comando e um job a mais
- Premissa: execução gratuita com gateway mock no CI e pastas locais de homologação/produção. Sem interface web, sem MLOps completo, sem fine-tuning, sem segundo provedor e sem deploy pago

## Capabilities

### New Capabilities

- `ai-delivery-pipeline`: esteira que valida, avalia, empacota e promove, e recusa release abaixo do gate
- `ai-artifact-versions`: versões rastreáveis de prompt, dataset, embedding, schema e configuração, com rollback
- `offline-quality-gates`: golden dataset operacional e métricas que decidem se a release passa
- `inference-run-trace`: registro de execução da inferência sem dado sensível
- `release-guardrails`: validação de entrada e saída, retry, fallback e intervenção humana

### Modified Capabilities

- (nenhuma main spec arquivada; o CI de manifesto e a triagem existente permanecem)

## Impact

- Novo pacote e CLI (ex.: `com.mundodev.filafacil.aidevops`), job extra no GitHub Actions, sem chave de API
- Golden dataset novo, fora da pasta `inference/golden` que o eval atual fixa em 3 casos
- Manifesto de release em arquivo, com ponteiro do que está ativo em homologação e produção simuladas
- Reuso do gateway mock e do redator de segredos já existentes
- README com o comando local da esteira e o significado de cada gate
- Referência: PDF *DevOps for AI Básico + Intermediário – FilaFácil Logística* (Escola Mundo Dev). O gráfico de pesos não extraiu números; o texto dá maior peso à avaliação offline e ao CI/CD
