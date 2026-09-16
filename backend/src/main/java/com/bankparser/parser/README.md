# Parser

**Fase 0/1 do DEVELOPMENT_PLAN.md** — implementar aqui.

Conteúdo esperado:
- `BankStatementParser.java` — interface abstrata
- `StoneParser.java` — implementação para extratos Stone
- `TransactionExtractor.java` — lógica de regex/parsing de valores e datas
- `dto/` — `StatementMetadata`, `Transaction`, `ParsingResult`

Referência da lógica original (Python, `pdfplumber`) que deve ser portada:
- Agrupamento de palavras por coordenada Y (tolerância ~2.5pt) para reconstruir linhas
- Identificação de linha de transação via regex de data (`dd/mm/yy`) + palavra "Entrada"/"Saída"
- Parsing de valores monetários brasileiros (`R$ 1.234,56`, com sinal opcional)
- Deduplicação de linhas de descrição consecutivas idênticas (artefato de renderização em negrito no PDF)
- Extração de CNPJ e data de emissão do cabeçalho (primeira página)

Este código deve ser testado isoladamente (ver `src/test/java/com/bankparser/parser/`) antes de integrar com API/DB.
