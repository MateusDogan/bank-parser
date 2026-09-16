package com.bankparser.exception;

/**
 * O CNPJ/CPF do cabecalho do extrato nao e o do cliente escolhido no upload.
 *
 * <p>Lancar em vez de aceitar: um extrato lancado sob o cliente errado e um
 * erro caro e silencioso num escritorio contabil — o dado fica plausivel e so
 * aparece na conferencia.
 */
public class DocumentMismatchException extends RuntimeException {

    public DocumentMismatchException(String message) {
        super(message);
    }
}
