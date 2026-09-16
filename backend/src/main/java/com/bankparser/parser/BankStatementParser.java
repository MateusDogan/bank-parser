package com.bankparser.parser;

import com.bankparser.parser.dto.ParsingResult;

import java.io.InputStream;

/**
 * Contrato para extracao de dados de um extrato bancario em PDF.
 * Cada banco/layout suportado tem sua propria implementacao (ex.: {@link StoneParser}),
 * permitindo adicionar novos bancos sem alterar o restante do sistema.
 */
public interface BankStatementParser {

    /** Identificador do banco/layout suportado (ex.: "stone"), usado para roteamento. */
    String bankKey();

    /**
     * Extrai metadados e transacoes do PDF.
     *
     * @throws StatementParsingException se o PDF nao puder ser lido, nao tiver paginas,
     *                                    ou nenhuma transacao reconhecivel for encontrada
     */
    ParsingResult parse(InputStream pdfInputStream);
}
