package com.bankparser.testsupport;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * PDFs de extrato gerados em memoria, reproduzindo o layout de colunas da
 * Stone. Usados no lugar de extratos reais para nao versionar dado financeiro
 * de cliente no repositorio.
 *
 * <p>Duas armadilhas ja pagas aqui, relevantes para quem for escrever fixtures
 * de outro banco:
 *
 * <ul>
 *   <li>As colunas usam espacamento horizontal generoso de proposito. Se duas
 *       palavras ficam visualmente proximas, o PDFBox as funde num unico trecho
 *       de texto na extracao e a divisao em palavras que o parser espera
 *       quebra.
 *   <li>A fonte e TrueType, nao uma Standard14: as Standard14 do PDFBox nao
 *       lidam com acentuacao (i, a, c com cedilha) de forma confiavel, e o
 *       parser procura exatamente "Saida" com acento.
 * </ul>
 */
public final class SyntheticStatementPdf {

    /** Documento no cabecalho do extrato gerado por {@link #withTwoTransactions()}. */
    public static final String DOCUMENT = "12.345.678/0001-99";

    private static final float FONT_SIZE = 9f;

    /**
     * Fonte que o proprio PDFBox empacota como fallback. Vem do classpath, e nao
     * de um caminho do sistema, porque a suite roda no Windows e no CI (Ubuntu),
     * onde nenhum caminho de fonte serve para os dois. A Liberation Sans e
     * metricamente compativel com a Arial, entao as coordenadas que o parser le
     * nao mudam.
     */
    private static final String FONT_RESOURCE =
            "/org/apache/pdfbox/resources/ttf/LiberationSans-Regular.ttf";

    private SyntheticStatementPdf() {
    }

    /** Extrato com uma Saida e uma Entrada, emitido em 27/08/2026. */
    public static byte[] withTwoTransactions() throws IOException {
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
                writeWord(cs, font, DOCUMENT, 300, y);

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

    /**
     * Extrato cuja unica transacao tem a coluna de valor ilegivel — simula a
     * Stone mudando o layout de um jeito que o parser nao acompanha.
     */
    public static byte[] withIllegibleAmount() throws IOException {
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
                writeWord(cs, font, DOCUMENT, 300, y);

                y -= 40;
                writeWord(cs, font, "DATA", 40, y);
                writeWord(cs, font, "TIPO", 160, y);

                y -= 25;
                writeWord(cs, font, "26/08/26", 40, y);
                writeWord(cs, font, "Saída", 160, y);
                // Sem digitos na faixa X da coluna de valor.
                writeWord(cs, font, "R$", 280, y);
                writeWord(cs, font, "—", 320, y);
                writeWord(cs, font, "R$", 420, y);
                writeWord(cs, font, "2.321,30", 460, y);
            }

            return toBytes(document);
        }
    }

    /** PDF valido mas sem nenhuma linha de transacao reconhecivel. */
    public static byte[] headerOnly() throws IOException {
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
        try (InputStream font = SyntheticStatementPdf.class.getResourceAsStream(FONT_RESOURCE)) {
            if (font == null) {
                throw new IOException("Fonte de teste ausente no classpath: " + FONT_RESOURCE
                        + " (o PDFBox mudou de layout de recursos?)");
            }
            return PDType0Font.load(document, font);
        }
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
