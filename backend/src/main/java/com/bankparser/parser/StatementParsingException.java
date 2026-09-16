package com.bankparser.parser;

/**
 * Lancada quando um PDF nao pode ser lido ou seu layout nao e reconhecido
 * pelo parser. Preferimos falhar de forma explicita a persistir dados
 * extraidos incorretamente.
 */
public class StatementParsingException extends RuntimeException {

    public StatementParsingException(String message) {
        super(message);
    }

    public StatementParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
