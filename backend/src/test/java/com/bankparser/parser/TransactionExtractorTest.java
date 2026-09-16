package com.bankparser.parser;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionExtractorTest {

    @Test
    void parsesPositiveMoney() {
        assertThat(TransactionExtractor.parseMoney("R$ 1.234,56"))
                .isEqualByComparingTo("1234.56");
    }

    @Test
    void parsesNegativeMoneyWithExplicitSign() {
        assertThat(TransactionExtractor.parseMoney("- R$ 1.234,56"))
                .isEqualByComparingTo("-1234.56");
    }

    @Test
    void parsesSmallValueWithoutThousandsSeparator() {
        assertThat(TransactionExtractor.parseMoney("R$ 0,01"))
                .isEqualByComparingTo("0.01");
    }

    @Test
    void returnsNullWhenNoNumberPresent() {
        assertThat(TransactionExtractor.parseMoney("texto sem numero")).isNull();
        assertThat(TransactionExtractor.parseMoney("")).isNull();
        assertThat(TransactionExtractor.parseMoney(null)).isNull();
    }

    @Test
    void parsesDateAssumingYear2000Century() {
        assertThat(TransactionExtractor.parseDateDDMMYY("05", "08", "26"))
                .isEqualTo(LocalDate.of(2026, 8, 5));
    }

    @Test
    void resolvesPortugueseMonthNamesCaseInsensitively() {
        assertThat(TransactionExtractor.monthFromPortugueseName("agosto")).isEqualTo(8);
        assertThat(TransactionExtractor.monthFromPortugueseName("Janeiro")).isEqualTo(1);
        assertThat(TransactionExtractor.monthFromPortugueseName("março")).isEqualTo(3);
    }

    @Test
    void returnsNullForUnknownMonthName() {
        assertThat(TransactionExtractor.monthFromPortugueseName("mesinexistente")).isNull();
        assertThat(TransactionExtractor.monthFromPortugueseName(null)).isNull();
    }
}
