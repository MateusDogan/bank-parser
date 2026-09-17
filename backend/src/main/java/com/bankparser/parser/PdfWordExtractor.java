package com.bankparser.parser;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Extrai palavras posicionadas do PDF, agrupadas por pagina (uma lista por
 * pagina, na ordem do documento — inclusive paginas sem texto).
 *
 * PDFBox fornece posicoes por caractere. Para agrupar em palavras, sobrescrevemos
 * {@link #writeString}: recebe um trecho de texto com a posicao de cada caractere,
 * e dividimos nos espacos em branco.
 */
class PdfWordExtractor extends PDFTextStripper {

    private final List<List<PositionedWord>> wordsByPage = new ArrayList<>();
    private List<PositionedWord> currentPageWords;

    PdfWordExtractor() throws IOException {
        super();
        setSortByPosition(true);
    }

    List<List<PositionedWord>> extract(PDDocument document) throws IOException {
        wordsByPage.clear();
        currentPageWords = null;
        getText(document);
        return wordsByPage;
    }

    @Override
    protected void startPage(PDPage page) throws IOException {
        currentPageWords = new ArrayList<>();
        wordsByPage.add(currentPageWords);
        super.startPage(page);
    }

    @Override
    protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
        if (currentPageWords == null) {
            // Defensivo: nao deveria ocorrer, pois startPage() sempre precede o texto da pagina.
            currentPageWords = new ArrayList<>();
            wordsByPage.add(currentPageWords);
        }

        if (textPositions.size() != text.length()) {
            // Ligaduras/normalizacao podem quebrar o alinhamento 1:1 caractere<->posicao.
            // Fallback: trata o trecho inteiro como uma unica palavra.
            if (!textPositions.isEmpty() && !text.isBlank()) {
                TextPosition first = textPositions.get(0);
                currentPageWords.add(new PositionedWord(text.trim(), first.getXDirAdj(), first.getYDirAdj()));
            }
            return;
        }

        int start = 0;
        int length = text.length();
        for (int i = 0; i <= length; i++) {
            boolean atEnd = i == length;
            boolean isSpace = !atEnd && Character.isWhitespace(text.charAt(i));
            if (atEnd || isSpace) {
                if (i > start) {
                    String word = text.substring(start, i);
                    TextPosition first = textPositions.get(start);
                    currentPageWords.add(new PositionedWord(word, first.getXDirAdj(), first.getYDirAdj()));
                }
                start = i + 1;
            }
        }
    }
}
