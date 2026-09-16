package com.bankparser.parser;

import com.bankparser.parser.dto.ParsedTransaction;
import com.bankparser.parser.dto.ParsingResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes de integracao do {@link StoneParser} usando PDFs sinteticos gerados
 * em memoria com PDFBox (reproduzindo o layout de colunas da Stone), em vez
 * de extratos reais — evita commitar dados financeiros sensiveis no
 * repositorio enquanto valida o pipeline completo (extracao de palavras +
 * agrupamento de linhas + parsing por coordenada).
 *
 * <p>As colunas usam espacamento generoso de proposito: se duas palavras
 * ficarem visualmente proximas o suficiente para seus retangulos se
 * sobreporem, o PDFBox pode funde-las em um unico trecho de texto na
 * extracao, quebrando a divisao em "palavras" que o parser depende.
 *
 * <p>Fixtures com PDFs reais (idealmente anonimizados) podem ser adicionadas
 * depois em {@code src/test/resources/parser/} — ver README daquela pasta.
 */
class StoneParserTest {

    private static final float FONT_SIZE = 9f;
    private static final String SYSTEM_FONT_PATH = "C:\\Windows\\Fonts\\arial.ttf";

    private final StoneParser parser = new StoneParser();

    @Test
    void parsesEntradaAndSaidaTransactionsWithMetadata() throws IOException {
        byte[] pdf = buildSyntheticStatement();

        ParsingResult result = parser.parse(new ByteArrayInputStream(pdf));

        assertThat(result.metadata().documento()).isEqualTo("12.345.678/0001-99");
        assertThat(result.metadata().emitidoEm()).isEqualTo(LocalDate.of(2026, 8, 27));

        List<ParsedTransaction> transactions = result.transactions();
        assertThat(transactions).hasSize(2);

        ParsedTransaction saida = transactions.get(0);
        assertThat(saida.data()).isEqualTo(LocalDate.of(2026, 8, 26));
        assertThat(saida.tipo()).isEqualTo("Saída");
        assertThat(saida.valor()).isEqualByComparingTo("-300.00");
        assertThat(saida.saldo()).isEqualByComparingTo("2321.30");
        assertThat(saida.descricao()).contains("CLIENTE TESTE LTDA");
        assertThat(saida.detalhe()).isEqualTo("Transferência | Pix");

        ParsedTransaction entrada = transactions.get(1);
        assertThat(entrada.data()).isEqualTo(LocalDate.of(2026, 8, 17));
        assertThat(entrada.tipo()).isEqualTo("Entrada");
        assertThat(entrada.valor()).isEqualByComparingTo("485.70");
        assertThat(entrada.saldo()).isEqualByComparingTo("32321.29");
        assertThat(entrada.detalhe()).isEqualTo("Visa | Crédito");
    }

    @Test
    void throwsWhenNoTransactionsAreFound() throws IOException {
        byte[] pdf = buildPdfWithOnlyHeader();

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

    // --- construcao de PDFs sinteticos ---------------------------------

    private byte[] buildSyntheticStatement() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDFont font = loadUnicodeFont(document);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float y = PDRectangle.A4.getHeight() - 50;

                writeWord(cs, font, "Nome", 40, y);
                writeWord(cs, font, "Documento", 250, y);

                y -= 20;
                writeWord(cs, font, "Cliente Teste", 40, y);
                writeWord(cs, font, "12.345.678/0001-99", 300, y);

                y -= 20;
                writeWord(cs, font, "Emitido", 40, y);
                writeWord(cs, font, "em", 130, y);
                writeWord(cs, font, "27", 180, y);
                writeWord(cs, font, "agosto", 230, y);
                writeWord(cs, font, "2026", 320, y);

                y -= 40;
                writeWord(cs, font, "DATA", 40, y);
                writeWord(cs, font, "TIPO", 160, y);

                y -= 25;
                writeWord(cs, font, "CLIENTE", 40, y);
                writeWord(cs, font, "TESTE", 150, y);
                writeWord(cs, font, "LTDA", 260, y);

                y -= 20;
                writeWord(cs, font, "26/08/26", 40, y);
                writeWord(cs, font, "Saída", 160, y);
                writeWord(cs, font, "R$", 280, y);
                writeWord(cs, font, "300,00", 320, y);
                writeWord(cs, font, "R$", 420, y);
                writeWord(cs, font, "2.321,30", 460, y);

                y -= 20;
                writeWord(cs, font, "Transferência", 40, y);
                writeWord(cs, font, "|", 200, y);
                writeWord(cs, font, "Pix", 220, y);

                y -= 20;
                writeWord(cs, font, "17/08/26", 40, y);
                writeWord(cs, font, "Entrada", 160, y);
                writeWord(cs, font, "R$", 280, y);
                writeWord(cs, font, "485,70", 320, y);
                writeWord(cs, font, "R$", 420, y);
                writeWord(cs, font, "32.321,29", 460, y);

                y -= 20;
                writeWord(cs, font, "Visa", 40, y);
                writeWord(cs, font, "|", 120, y);
                writeWord(cs, font, "Crédito", 140, y);
            }

            return toBytes(document);
        }
    }

    private byte[] buildPdfWithOnlyHeader() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDFont font = loadUnicodeFont(document);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float y = PDRectangle.A4.getHeight() - 50;
                writeWord(cs, font, "Extrato", 40, y);
                y -= 20;
                writeWord(cs, font, "Stone", 40, y);
            }

            return toBytes(document);
        }
    }

    private static PDFont loadUnicodeFont(PDDocument document) throws IOException {
        // Fonte TrueType do sistema, para suportar acentuacao (í, é, ã, ç) sem
        // as limitacoes de encoding das fontes Standard14 (Type1/AFM) do PDFBox.
        return PDType0Font.load(document, new File(SYSTEM_FONT_PATH));
    }

    private static byte[] toBytes(PDDocument document) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        document.save(out);
        return out.toByteArray();
    }

    private static void writeWord(PDPageContentStream cs, PDFont font, String text, float x, float y)
            throws IOException {
        cs.beginText();
        cs.setFont(font, FONT_SIZE);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }
}
