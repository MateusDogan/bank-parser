package com.bankparser;

import com.bankparser.parser.BankStatementParser;
import com.bankparser.parser.StoneParser;
import com.bankparser.parser.dto.ParsingResult;
import com.bankparser.util.CsvExporter;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Teste de ponta a ponta do pipeline PDF -> {@link CsvExporter}, executado
 * apenas quando ha um extrato real disponivel na maquina. O caminho do PDF
 * vem da propriedade de sistema {@code bankparser.it.pdf} (ex.:
 * {@code mvn test -Dbankparser.it.pdf=/caminho/extrato.pdf}) e, na ausencia
 * dela, do fixture convencional em {@code src/test/resources/parser/} — ver
 * README daquela pasta.
 *
 * <p>Sem fixture o teste e ignorado via {@code assumeTrue} em vez de falhar:
 * extratos reais nao sao versionados, entao qualquer outra maquina (e o CI)
 * simplesmente nao os tem. A cobertura do parser em si nao depende deste
 * teste — {@link com.bankparser.parser.StoneParserTest} usa PDFs sinteticos.
 */
class CsvExportIntegrationTest {

    private static final String PDF_PATH_PROPERTY = "bankparser.it.pdf";
    private static final Path DEFAULT_PDF_FIXTURE =
            Paths.get("src", "test", "resources", "parser", "extrato_stone_01.pdf");
    private static final Path CSV_OUT =
            Paths.get("target", "test-output", "extrato-java-gerado.csv");

    @Test
    void exportPdfToCsv() throws IOException {
        Path pdf = resolvePdfFixture();
        assumeTrue(Files.isReadable(pdf),
                "PDF de fixture ausente (" + pdf + "); defina -D" + PDF_PATH_PROPERTY + " para executar");

        ParsingResult result;
        try (InputStream pdfStream = Files.newInputStream(pdf)) {
            BankStatementParser parser = new StoneParser();
            result = parser.parse(pdfStream);
        }

        CsvExporter.exportToCsv(result, CSV_OUT.toString());

        List<String> lines = Files.readAllLines(CSV_OUT, StandardCharsets.UTF_8);
        assertThat(lines).isNotEmpty();
        assertThat(lines.get(0)).isEqualTo("data;tipo;valor;saldo;descricao;detalhe");
        assertThat(lines).hasSize(result.transactions().size() + 1);
    }

    private static Path resolvePdfFixture() {
        String configured = System.getProperty(PDF_PATH_PROPERTY);
        return configured != null && !configured.isBlank()
                ? Paths.get(configured)
                : DEFAULT_PDF_FIXTURE;
    }
}
