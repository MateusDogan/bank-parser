package com.bankparser;

import com.bankparser.entity.Statement;
import com.bankparser.entity.Transaction;
import com.bankparser.parser.StoneParser;
import com.bankparser.parser.dto.ParsedTransaction;
import com.bankparser.parser.dto.ParsingResult;
import com.bankparser.validation.BalanceValidationService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Baseline do parser contra o extrato real, para responder a pergunta que
 * motivou a Fase 0.5: "se eu mexer no parser, como sei que nao quebrei nada?".
 *
 * <p>Os PDFs sinteticos dos outros testes tem layout perfeito e por isso nao
 * reproduzem os defeitos que so aparecem no arquivo de verdade. Este teste fixa
 * os dois numeros conhecidos do extrato real e falha se qualquer um mudar —
 * para melhor ou para pior. Uma falha aqui nao e um bug do teste: e o aviso de
 * que a saida do parser mudou e alguem precisa olhar o diff antes de atualizar
 * a constante de proposito (e, junto com ela, {@code StoneParser.parserVersion()}).
 *
 * <p><b>O extrato real nunca e versionado</b> (dado financeiro de cliente).
 * Sem ele o teste e ignorado, aqui e no CI:
 * {@code mvn test -Dbankparser.it.pdf=C:\caminho\extrato.pdf}
 *
 * <p><b>Constantes confirmadas na primeira execução local.</b> As 264 transações
 * e 11 divergências de saldo são a baseline deste extrato. Se o número sair
 * diferente na primeira rodada, o valor a corrigir é o daqui, não o código.
 */
class RealStatementRegressionTest {

    private static final String PDF_PATH_PROPERTY = "bankparser.it.pdf";
    private static final Path DEFAULT_PDF_FIXTURE =
            Paths.get("src", "test", "resources", "parser", "extrato_stone_01.pdf");

    private static final int EXPECTED_TRANSACTIONS = 264;
    private static final int EXPECTED_BALANCE_BREAKS = 11;

    @Test
    void parserOutputMatchesTheKnownBaseline() throws IOException {
        Path pdf = resolvePdfFixture();
        assumeTrue(Files.isReadable(pdf),
                "Extrato real ausente (" + pdf + "); defina -D" + PDF_PATH_PROPERTY + " para executar");

        ParsingResult result;
        try (InputStream in = Files.newInputStream(pdf)) {
            result = new StoneParser().parse(in);
        }

        assertThat(result.transactions())
                .as("numero de transacoes extraidas do extrato real")
                .hasSize(EXPECTED_TRANSACTIONS);

        BalanceValidationService.ValidationReport report =
                new BalanceValidationService().checkBalanceContinuity(asEntities(result.transactions()));

        assertThat(report.divergentLines())
                .as("quebras de continuidade de saldo (baseline conhecida: %d). "
                        + "Linhas acusadas agora: %s", EXPECTED_BALANCE_BREAKS, report.divergentLines())
                .hasSize(EXPECTED_BALANCE_BREAKS);
    }

    /** A checagem de saldo opera sobre entidades; aqui elas nao sao persistidas. */
    private static List<Transaction> asEntities(List<ParsedTransaction> parsed) {
        Statement statement = new Statement();
        List<Transaction> entities = new ArrayList<>(parsed.size());
        for (int i = 0; i < parsed.size(); i++) {
            ParsedTransaction source = parsed.get(i);
            Transaction transaction = new Transaction(statement, i);
            transaction.setTransactionDate(source.data());
            transaction.setType(source.tipo());
            transaction.setAmount(source.valor());
            transaction.setBalance(source.saldo());
            entities.add(transaction);
        }
        return entities;
    }

    private static Path resolvePdfFixture() {
        String configured = System.getProperty(PDF_PATH_PROPERTY);
        return configured != null && !configured.isBlank()
                ? Paths.get(configured)
                : DEFAULT_PDF_FIXTURE;
    }
}
