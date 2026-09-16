package com.bankparser.validation;

import com.bankparser.entity.Transaction;
import com.bankparser.parser.dto.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testa a validacao de continuidade de saldo.
 * Constroi transacoes em memoria com sequencias de saldo conhecidas.
 */
class BalanceValidationServiceTest {

    private final BalanceValidationService service = new BalanceValidationService();

    @Test
    void detectsNoBreaksInCleanSequence() {
        // Simple case: todas as transacoes verificam OK
        List<Transaction> txs = List.of(
                tx(1, bd("-100.00"), bd("1000.00")),
                tx(2, bd("-50.00"), bd("1100.00")),
                tx(3, bd("-25.00"), bd("1150.00"))
        );

        BalanceValidationService.ValidationReport report = service.checkBalanceContinuity(txs);

        assertThat(report.divergentLines()).isEmpty();
        assertThat(report.toStorageSummary()).isNull();
    }

    @Test
    void detectsBreaksWhenSaldoInconsistent() {
        // Apenas teste que a lista de divergencias nao e vazia quando ha problema
        List<Transaction> txs = List.of(
                tx(1, bd("-100.00"), bd("1000.00")),
                tx(2, bd("-50.00"), bd("999.00"))  // Deveria ser 1050, mas e 999
        );

        BalanceValidationService.ValidationReport report = service.checkBalanceContinuity(txs);

        assertThat(report.divergentLines()).isNotEmpty();
        assertThat(report.toStorageSummary()).isNotNull();
    }

    @Test
    void ignoresTransactionsWithMissingBalance() {
        List<Transaction> txs = List.of(
                tx(1, bd("100.00"), bd("1000.00")),
                tx(2, bd("50.00"), null), // Saldo nulo, ignora comparacao
                tx(3, bd("25.00"), bd("900.00"))
        );

        BalanceValidationService.ValidationReport report = service.checkBalanceContinuity(txs);

        // Nao reporta linha 2 porque falta dado, e nao consegue validar linha 3
        assertThat(report.divergentLines()).isEmpty();
    }

    private Transaction tx(int lineNumber, BigDecimal value, BigDecimal balance) {
        Transaction t = new Transaction();
        t.setId(UUID.randomUUID());
        t.setLineNumber(lineNumber);
        t.setAmount(value);
        t.setBalance(balance);
        t.setType(TransactionType.SAIDA);
        t.setDescription("Test");
        t.setDetail("");
        t.setTransactionDate(LocalDate.now());
        t.setCreatedAt(Instant.now());
        t.setUpdatedAt(Instant.now());
        t.setDeleted(false);
        return t;
    }

    private BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
