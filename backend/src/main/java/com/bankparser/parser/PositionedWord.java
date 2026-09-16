package com.bankparser.parser;

/**
 * Uma palavra extraida do PDF com sua posicao: {@code x0} e a borda esquerda
 * (pontos, a partir da esquerda da pagina) e {@code top} e a distancia do
 * topo da pagina (cresce para baixo) — equivalente ao que
 * {@code page.extract_words()} do pdfplumber (Python) fornece.
 */
record PositionedWord(String text, float x0, float top) {
}
