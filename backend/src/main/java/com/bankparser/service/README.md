# Services

- `StatementProcessingService.java` — orquestra o fluxo de upload: escolhe o parser pelo `bankKey`,
  grava o PDF no storage e persiste `Statement` + `Transaction`s

## Detalhes que não são óbvios no código

**Ordem**: o PDF vai para o storage *antes* do commit. Falha no storage aborta a transação e deixa no
máximo um objeto órfão no bucket; na ordem inversa sobraria um `Statement` apontando para um arquivo
inexistente.

**Bytes lidos uma vez**: o `InputStream` do multipart não é relido, e os mesmos bytes servem ao parse
e ao upload — daí o `byte[]`.
