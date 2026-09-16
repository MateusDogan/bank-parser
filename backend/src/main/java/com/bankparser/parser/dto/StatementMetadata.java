package com.bankparser.parser.dto;

import java.time.LocalDate;

/**
 * Metadados lidos do cabecalho do extrato (primeira pagina), usados para
 * identificar/nomear o extrato processado.
 */
public record StatementMetadata(
        String documento,
        LocalDate emitidoEm
) {
}
