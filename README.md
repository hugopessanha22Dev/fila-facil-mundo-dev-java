# FilaFacil Logistica

Aplicação Java de **terminal** para gerenciar solicitações de coleta, com persistência em arquivo local JSON.

Case: Escola Mundo Dev — Desenvolvimento de Software Básico.

## Pré-requisitos

- JDK 21+
- Apache Maven 3.9+

Não é necessário MySQL, banco de dados ou frameworks de aplicação.

## Executar

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.11"
mvn -q compile exec:java
```

Na primeira execução, se `data/coletas.json` ainda não existir, o sistema inicia com lista vazia e cria o arquivo no primeiro cadastro.

## Persistência

- Arquivo: `data/coletas.json`
- Formato: lista JSON de coletas (`id`, `nome`, `endereco`, `pacotes`, `prioridade`, `status`)
- Carregado ao iniciar; gravado após cadastro ou atualização de status

## Menu

| Opção | Ação |
|-------|------|
| 1 | Cadastrar coleta (status inicial `pendente`) |
| 2 | Listar todas as coletas |
| 3 | Filtrar por status ou prioridade |
| 4 | Atualizar status por ID |
| 5 | Resumo operacional |
| 0 | Sair |

### Valores aceitos

- **Prioridade:** `baixa`, `media`, `alta`
- **Status:** `pendente`, `em_rota`, `concluida`

## Arquitetura

```
controller (MenuController)
  → service (ColetaService — validações, filter/map/reduce)
    → store (ColetaFileStore — ler/gravar JSON)
```

## Estrutura

```
src/main/java/com/mundodev/filafacil/
  Main.java
  model/Coleta.java
  store/ColetaFileStore.java
  service/ColetaService.java
  controller/MenuController.java
data/coletas.json   # gerado em runtime
pom.xml
```
