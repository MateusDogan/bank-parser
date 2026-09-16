package com.bankparser.storage;

/** Falha ao gravar ou ler um arquivo no storage. */
public class StorageException extends RuntimeException {

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
