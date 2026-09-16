package com.bankparser.parser.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Uma transacao extraida de um extrato bancario.
 *
 * @param valor    positivo para Entrada, negativo para Saida
 * @param saldo    saldo da conta apos a transacao
 * @param descricao texto/contraparte associado (nem sempre perfeitamente
 *                  separado de {@code detalhe} por causa de quebras de linha
 *                  irregulares no PDF de origem)
 * @param detalhe  texto complementar (forma de pagamento, bandeira, canal, etc.)
 */
public record ParsedTransaction(
        LocalDate data,
        String tipo,
        BigDecimal valor,
        BigDecimal saldo,
        String descricao,
        String detalhe
) {
}
