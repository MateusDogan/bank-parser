# DTOs

DTOs de API, separados das entidades JPA de propósito: o contrato exposto não muda quando o schema do
banco muda (decisão de arquitetura #4).

- `StatementResponse.java` — extrato processado, com `StatementResponse.from(Statement)`
- `ErrorResponse.java` — corpo de erro padronizado do `GlobalExceptionHandler`

Nota: os DTOs do **parser** (`parser/dto/`) são outra coisa — representam o que foi extraído do PDF,
não o contrato HTTP. `ParsedTransaction` é reusado na exportação de CSV porque o `CsvExporter` já
trabalha sobre ele.
