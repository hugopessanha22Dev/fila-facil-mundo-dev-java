# ADR 0001: orquestrador de exceções

## Contexto

Cliente ausente, endereço divergente, pacote danificado, entrega fora da janela, rota bloqueada e reagendamento precisam de um ciclo único. O desenho cabe em memória, sem banco e sem fila real.

## Comparação 1: modelo anêmico ou entidade rica

A versão inicial deixava a transição dentro do comando de entrada: a CLI lia o status e decidia o próximo. Isso espalhava a regra pelo adaptador.

A opção adotada move a transição para a entidade. O adaptador só chama o caso de uso e imprime o resultado. A consequência é que nenhum adaptador carrega regra de domínio.

## Comparação 2: vocabulário único ou contextos traduzidos

Um vocabulário único faria a entidade aceitar o termo cru de cada área. Roteirização, atendimento e financeiro passariam a ditar o modelo.

A opção adotada separa os contextos. A linguagem publicada entra por um gateway, que traduz o termo para tipo e origem. Termo sem tradução não abre exceção.

## Evolução

Os casos de uso continuam sendo abrir, analisar, escalar, resolver, cancelar e consultar. O que mudou da versão inicial para a adotada foi o lugar da regra, não o contrato desses casos de uso.
