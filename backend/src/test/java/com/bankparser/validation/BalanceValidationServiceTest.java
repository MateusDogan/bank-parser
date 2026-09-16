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
 * Extratos vem do mais recente para o mais antigo, entao o invariante conferido
 * e {@code saldo[i] == saldo[i+1] + valor[i]}.
 *
 * <p>As sequencias abaixo partem sempre deste conjunto consistente e quebram
 * um {@code valor} de cada vez — {@code valor[i]} so participa da checagem da
 * propria linha {@code i}, entao da pra prever exatamente quais linhas devem
 * ser acusadas. Conferir quais linhas saem no relatorio, e nao so que ele nao
 * esta vazio, importa porque sao esses numeros que vao para
 * {@code validation_flags} e orientam a conferencia manual.
 */
class BalanceValidationServiceTest {

    private final BalanceValidationService service = new BalanceValidationService();

    /** Sequencia consistente: 1000-100=900, 1050-50=1000, 850+200=1050. */
    private List<Transaction> consistentSequence() {
        return List.of(
                tx(0, bd("-100.00"), bd("900.00")),
                tx(1, bd("-50.00"), bd("1000.00")),
                tx(2, bd("200.00"), bd("1050.00")),
                tx(3, bd("-25.00"), bd("850.00"))
        );
    }

    @Test
    void reportsNothingWhenEveryBalanceFollowsFromTheNext() {
        BalanceValidationService.ValidationReport report =
                service.checkBalanceContinuity(consistentSequence());

        assertThat(report.divergentLines()).isEmpty();
        assertThat(report.toStorageSummary()).isNull();
    }

    @Test
    void namesTheSingleLineThatBreaks() {
        List<Transaction> txs = List.of(
                tx(0, bd("-100.00"), bd("900.00")),
                tx(1, bd("-70.00"), bd("1000.00")), // esperado 1050-70=980, tem 1000
                tx(2, bd("200.00"), bd("1050.00")),
                tx(3, bd("-25.00"), bd("850.00"))
        );

        BalanceValidationService.ValidationReport report = service.checkBalanceContinuity(txs);

        assertThat(report.divergentLines()).containsExactly(1);
        assertThat(report.toStorageSummary()).isEqualTo("1 divergencia(s) de saldo nas linhas: 1");
    }

    @Test
    void namesEveryLineThatBreaks() {
        List<Transaction> txs = List.of(
                tx(0, bd("-80.00"), bd("900.00")),   // esperado 1000-80=920, tem 900
                tx(1, bd("-50.00"), bd("1000.00")),
                tx(2, bd("150.00"), bd("1050.00")),  // esperado 850+150=1000, tem 1050
                tx(3, bd("-25.00"), bd("850.00"))
        );

        BalanceValidationService.ValidationReport report = service.checkBalanceContinuity(txs);

        assertThat(report.divergentLines()).containsExactly(0, 2);
        assertThat(report.toStorageSummary()).isEqualTo("2 divergencia(s) de saldo nas linhas: 0, 2");
    }

    @Test
    void neverAccusesTheOldestTransaction() {
        // A ultima linha nao tem uma seguinte com que se comparar; um saldo
        // absurdo nela nao pode virar divergencia.
        List<Transaction> txs = List.of(
                tx(0, bd("-100.00"), bd("900.00")),
                tx(1, bd("-50.00"), bd("1000.00")),
                tx(2, bd("200.00"), bd("1050.00")),
                tx(3, bd("-25.00"), bd("999999.00"))
        );

        BalanceValidationService.ValidationReport report = service.checkBalanceContinuity(txs);

        assertThat(report.divergentLines()).doesNotContain(3);
    }

    @Test
    void skipsComparisonsWhereTheBalanceIsMissing() {
        // saldo e coluna opcional: sem ele nao da para afirmar nada, e chutar
        // divergencia encheria o relatorio de ruido.
        List<Transaction> txs = List.of(
                tx(0, bd("-100.00"), bd("900.00")),
                tx(1, bd("-50.00"), null),
                tx(2, bd("200.00"), bd("1050.00"))
        );

        BalanceValidationService.ValidationReport report = service.checkBalanceContinuity(txs);

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
