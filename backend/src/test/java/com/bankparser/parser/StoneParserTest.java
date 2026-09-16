package com.bankparser.parser;

import com.bankparser.parser.dto.ParsedTransaction;
import com.bankparser.parser.dto.ParsingResult;
import com.bankparser.parser.dto.TransactionType;
import com.bankparser.testsupport.SyntheticStatementPdf;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes de integracao do {@link StoneParser} sobre PDFs sinteticos — ver
 * {@link SyntheticStatementPdf} para como os extratos sao montados e por que.
 */
class StoneParserTest {

    private final StoneParser parser = new StoneParser();

    @Test
    void parsesEntradaAndSaidaTransactionsWithMetadata() throws IOException {
        byte[] pdf = SyntheticStatementPdf.withTwoTransactions();

        ParsingResult result = parser.parse(new ByteArrayInputStream(pdf));

        assertThat(result.metadata().documento()).isEqualTo(SyntheticStatementPdf.DOCUMENT);
        assertThat(result.metadata().emitidoEm()).isEqualTo(LocalDate.of(2026, 8, 27));

        List<ParsedTransaction> transactions = result.transactions();
        assertThat(transactions).hasSize(2);

        ParsedTransaction saida = transactions.get(0);
        assertThat(saida.data()).isEqualTo(LocalDate.of(2026, 8, 26));
        assertThat(saida.tipo()).isEqualTo(TransactionType.SAIDA);
        assertThat(saida.valor()).isEqualByComparingTo("-300.00");
        assertThat(saida.saldo()).isEqualByComparingTo("2321.30");
        assertThat(saida.descricao()).contains("CLIENTE TESTE LTDA");
        assertThat(saida.detalhe()).isEqualTo("Transferência | Pix");

        ParsedTransaction entrada = transactions.get(1);
        assertThat(entrada.data()).isEqualTo(LocalDate.of(2026, 8, 17));
        assertThat(entrada.tipo()).isEqualTo(TransactionType.ENTRADA);
        assertThat(entrada.valor()).isEqualByComparingTo("485.70");
        assertThat(entrada.saldo()).isEqualByComparingTo("32321.29");
        assertThat(entrada.detalhe()).isEqualTo("Visa | Crédito");
    }

    @Test
    void parserVersionIsExposed() {
        assertThat(parser.parserVersion()).isEqualTo("1.0");
    }

    @Test
    void throwsWhenNoTransactionsAreFound() throws IOException {
        byte[] pdf = SyntheticStatementPdf.headerOnly();

        assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream(pdf)))
                .isInstanceOf(StatementParsingException.class)
                .hasMessageContaining("Nenhuma transacao");
    }

    @Test
    void throwsOnEmptyOrCorruptedFile() {
        byte[] garbage = "isso nao e um pdf".getBytes();

        assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream(garbage)))
                .isInstanceOf(StatementParsingException.class);
    }
}
