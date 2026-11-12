# Mapa de contextos

A gestão de exceções é o contexto principal. Ela decide abertura, análise, escala, resolução e cancelamento.

| Contexto | Responsabilidade | Relação com a gestão de exceções |
| --- | --- | --- |
| Gestão de exceções | Ciclo de vida da exceção de entrega | Núcleo |
| Roteirização | Bloqueio de rota e janela de percurso | Upstream |
| Atendimento | Ausência do cliente e divergência de endereço | Upstream |
| Financeiro | Cobrança ligada a pacote danificado | Upstream |
| Auditoria operacional | Trilha do que aconteceu com a exceção | Downstream |

Roteirização, atendimento e financeiro publicam termos próprios. A gestão de exceções só recebe tipo e origem depois da tradução. A auditoria não importa a entidade: ela consome os eventos já publicados, com vocabulário de trilha.
