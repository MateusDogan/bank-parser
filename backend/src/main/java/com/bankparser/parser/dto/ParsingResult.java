package com.bankparser.parser.dto;

import java.util.List;

/** Resultado completo da extracao de um extrato: metadados + transacoes. */
public record ParsingResult(
        StatementMetadata metadata,
        List<ParsedTransaction> transactions
) {
}
