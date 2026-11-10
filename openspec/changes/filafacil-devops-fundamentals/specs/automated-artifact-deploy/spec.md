# Spec Delta

## Purpose

Entrega automaticamente uma versao utilizavel do validador apos o pipeline bem-sucedido, via artefato compactado e/ou release no repositorio, sem exigir cloud produtiva ou Kubernetes.

## ADDED Requirements

### Requirement: Artefato apos sucesso
Apos o CI concluir com sucesso, o fluxo MUST gerar um artefato compactado (por exemplo ZIP/JAR empacotado) disponivel para download.

#### Scenario: Build verde
- **WHEN** o pipeline completa sem falhas
- **THEN** um artefato compactado e publicado como output do workflow ou release

### Requirement: Separacao CI vs deploy
O repositorio MUST documentar e manter separacao clara entre integracao continua (validacao automatica) e deploy automatizado (entrega do artefato/release).

#### Scenario: README do fluxo
- **WHEN** um desenvolvedor le a secao de CI/CD no README
- **THEN** encontra explicacao de quando o CI roda e como o artefato/release e gerado

### Requirement: Tag de versao
O projeto MUST registrar ao menos uma tag de versao funcional documentada para marcar uma entrega estavel do validador.

#### Scenario: Tag presente
- **WHEN** a entrega DevOps e considerada concluida
- **THEN** existe orientacao/procedimento para criar ou verificar uma tag Git de versao do validador
