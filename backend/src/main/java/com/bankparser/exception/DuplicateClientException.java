package com.bankparser.exception;

/** Ja existe um cliente com o mesmo documento (CNPJ/CPF) nesta organizacao. */
public class DuplicateClientException extends RuntimeException {

    public DuplicateClientException(String message) {
        super(message);
    }
}
