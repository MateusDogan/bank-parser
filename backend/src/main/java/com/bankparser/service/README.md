# Services

**Fase 3 concluída**; `ClientService` / `TransactionService` / `ReportService` entram na Fase 4.

- `StatementProcessingService.java` — orquestra o fluxo de upload: carrega o `Client`, escolhe o
  parser pelo `bankKey`, confere o documento, grava o PDF no storage e persiste
  `Statement` + `Transaction`s
- `CurrentOrganizationProvider.java` — de qual organização é a requisição. Hoje devolve sempre a
  organização padrão; na Fase 7 passa a ler do `SecurityContext`. Existe para que essa mudança seja
  em **um lugar só**, em vez de espalhar `Organization.DEFAULT_ID` pelos controllers

## Detalhes que não são óbvios no código

**Ordem**: o PDF vai para o storage *antes* do commit. Falha no storage aborta a transação e deixa no
máximo um objeto órfão no bucket; na ordem inversa sobraria um `Statement` apontando para um arquivo
inexistente.

**Bytes lidos uma vez**: o `InputStream` do multipart não é relido, e os mesmos bytes servem ao parse
e ao upload — daí o `byte[]`.

**Conferência de documento**: CNPJ do extrato diferente do cliente escolhido lança
`DocumentMismatchException` (409). Documento ausente no PDF não bloqueia — não dá para conferir o que
o parser não achou.
