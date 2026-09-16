package com.bankparser.parser.dto;

/**
 * Direcao de uma transacao. Cada banco fala um vocabulario proprio no PDF
 * (Stone: "Entrada"/"Saída"; outros bancos podem falar "Credito"/"Debito") —
 * cada {@code BankStatementParser} mapeia o texto do seu banco para este
 * enum, entao o contrato da API nunca muda quando um novo banco entra.
 */
public enum TransactionType {
    ENTRADA("Entrada"),
    SAIDA("Saída");

    private final String label;

    TransactionType(String label) {
        this.label = label;
    }

    /**
     * Label acentuado ("Entrada"/"Saída"), nao o nome da constante Java.
     * Usado por {@code String.format("%s", ...)} no CSV — a persistencia via
     * {@code @Enumerated(EnumType.STRING)} usa {@link #name()}, nao isto, e
     * portanto nao e afetada.
     */
    @Override
    public String toString() {
        return label;
    }
}
