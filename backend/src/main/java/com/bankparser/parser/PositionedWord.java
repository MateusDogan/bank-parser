package com.bankparser.parser;

/**
 * Uma palavra extraida do PDF com sua posicao: {@code x0} (esquerda, em pontos)
 * e {@code top} (distancia do topo em pontos, cresce para baixo).
 */
record PositionedWord(String text, float x0, float top) {
}
