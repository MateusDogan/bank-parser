# Parser

**Fase 0/1 do DEVELOPMENT_PLAN.md — concluída.**

- `BankStatementParser.java` — interface abstrata (contrato para qualquer banco)
- `StoneParser.java` — implementação para extratos Stone (agrupamento de linhas por coordenada Y, extração por faixas de coordenada X, extração de metadados do cabeçalho)
- `TransactionExtractor.java` — utilitários reutilizáveis: parsing de moeda brasileira e datas
- `PositionedWord.java` / `PdfWordExtractor.java` — extração de palavras posicionadas via PDFBox
- `StatementParsingException.java` — erro explícito quando o PDF não é reconhecido
- `dto/` — `ParsedTransaction`, `StatementMetadata`, `ParsingResult`

**Regras de parsing Stone**:
- Agrupamento de palavras por coordenada Y (tolerância 2.5pt) para reconstruir linhas
- Identificação de linha de transação via regex de data (`dd/mm/yy`) + palavra "Entrada"/"Saída"
- Parsing de valores monetários brasileiros (`R$ 1.234,56`, com sinal opcional)
- Deduplicação de linhas de descrição consecutivas idênticas (artefato de renderização em negrito no PDF)
- Extração de CNPJ e data de emissão do cabeçalho (primeira página)

**Testes**: `src/test/java/com/bankparser/parser/` — `TransactionExtractorTest` (utilitários puros) e `StoneParserTest`
(integração, com PDFs sintéticos gerados via PDFBox para não commitar dados financeiros reais). Rodar com `mvn test`.

**Pendência opcional**: adicionar PDFs "golden" reais (idealmente anonimizados) em `src/test/resources/parser/`
para validar contra o layout real da Stone além dos sintéticos.
