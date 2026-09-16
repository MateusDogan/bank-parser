package com.bankparser.parser;

import com.bankparser.parser.dto.ParsedTransaction;
import com.bankparser.parser.dto.ParsingResult;
import com.bankparser.parser.dto.StatementMetadata;
import com.bankparser.parser.dto.TransactionType;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Parser para extratos da Stone (Instituicao de Pagamento).
 *
 * <p>O PDF de origem nao possui tabelas estruturadas; a extracao e feita por
 * posicionamento de texto (coordenadas x/y):
 * <ol>
 *   <li>Palavras sao agrupadas em linhas por proximidade vertical ({@link #Y_TOLERANCE});</li>
 *   <li>Uma linha e "central" (uma transacao) quando comeca com uma data
 *       (dd/MM/yy) seguida de "Entrada" ou "Saída";</li>
 *   <li>Dentro da linha central, faixas de coordenada X separam texto
 *       inline de contraparte, valor e saldo (layout fixo de colunas da Stone);</li>
 *   <li>Linhas entre duas transacoes (que nao sejam cabecalho) compoem a
 *       descricao; a linha imediatamente apos uma transacao, se nao for
 *       cabecalho nem outra transacao, e o "detalhe".</li>
 * </ol>
 */
@Component
public class StoneParser implements BankStatementParser {

    private static final float Y_TOLERANCE = 2.5f;

    @Override
    public String parserVersion() {
        return "1.0";
    }

    private static final Pattern DATE_PATTERN = Pattern.compile("^(\\d{2})/(\\d{2})/(\\d{2})$");
    private static final Pattern EMITIDO_EM_PATTERN =
            Pattern.compile("Emitido em (\\d{1,2}) (\\S+) (\\d{4})");

    private static final Set<String> HEADER_START_WORDS = Set.of(
            "Extrato", "Emitido", "Página", "Dados", "Nome", "Instituição", "Período:", "Stone", "DATA"
    );

    // Faixas de coordenada X (pontos) do layout Stone.
    private static final float INLINE_DESC_X_MIN = 110f;
    private static final float INLINE_DESC_X_MAX = 277f;
    private static final float VALOR_X_MIN = 277f;
    private static final float VALOR_X_MAX = 358f;
    private static final float SALDO_X_MIN = 358f;
    private static final float DOCUMENTO_X_MIN = 300f;

    @Override
    public String bankKey() {
        return "stone";
    }

    @Override
    public ParsingResult parse(InputStream pdfInputStream) {
        try {
            byte[] bytes = pdfInputStream.readAllBytes();
            try (PDDocument document = Loader.loadPDF(bytes)) {
                if (document.getNumberOfPages() == 0) {
                    throw new StatementParsingException("PDF nao contem paginas.");
                }

                List<List<PositionedWord>> pages = new PdfWordExtractor().extract(document);

                List<List<PositionedWord>> allLines = new ArrayList<>();
                for (List<PositionedWord> pageWords : pages) {
                    allLines.addAll(groupLines(pageWords));
                }

                List<List<PositionedWord>> firstPageLines = groupLines(pages.get(0));
                StatementMetadata metadata = extractAccountInfo(firstPageLines);

                List<ParsedTransaction> transactions = extractTransactions(allLines);
                if (transactions.isEmpty()) {
                    throw new StatementParsingException(
                            "Nenhuma transacao reconhecida no PDF. Verifique se o arquivo e um extrato Stone valido."
                    );
                }

                return new ParsingResult(metadata, transactions);
            }
        } catch (IOException e) {
            throw new StatementParsingException("Falha ao ler o PDF: " + e.getMessage(), e);
        }
    }

    /** Agrupa palavras de uma pagina em linhas por proximidade vertical (top). */
    static List<List<PositionedWord>> groupLines(List<PositionedWord> pageWords) {
        List<PositionedWord> sorted = new ArrayList<>(pageWords);
        sorted.sort(Comparator.comparingDouble(PositionedWord::top).thenComparingDouble(PositionedWord::x0));

        List<List<PositionedWord>> lines = new ArrayList<>();
        List<PositionedWord> current = new ArrayList<>();
        Float currentTop = null;

        for (PositionedWord w : sorted) {
            if (currentTop == null || Math.abs(w.top() - currentTop) <= Y_TOLERANCE) {
                current.add(w);
                if (currentTop == null) {
                    currentTop = w.top();
                }
            } else {
                lines.add(current);
                current = new ArrayList<>();
                current.add(w);
                currentTop = w.top();
            }
        }
        if (!current.isEmpty()) {
            lines.add(current);
        }
        return lines;
    }

    private static String lineText(List<PositionedWord> line) {
        return line.stream().map(PositionedWord::text).collect(Collectors.joining(" "));
    }

    private static boolean isHeaderLine(List<PositionedWord> line) {
        if (line.isEmpty()) {
            return true;
        }
        return HEADER_START_WORDS.contains(line.get(0).text());
    }

    private static boolean isCoreLine(List<PositionedWord> line) {
        if (line.size() <= 1) {
            return false;
        }
        boolean dateMatches = DATE_PATTERN.matcher(line.get(0).text()).matches();
        String tipo = line.get(1).text();
        return dateMatches && ("Entrada".equals(tipo) || "Saída".equals(tipo));
    }

    private static boolean isColumnHeaderLine(List<PositionedWord> line) {
        return line.size() > 1
                && "DATA".equals(line.get(0).text())
                && "TIPO".equals(line.get(1).text());
    }

    /** Dados extraidos apenas da linha central (antes de agregar descricao/detalhe das linhas vizinhas). */
    private record CoreLineData(LocalDate data, String tipo, String inlineText, BigDecimal valor, BigDecimal saldo) {
    }

    private CoreLineData parseCoreLine(List<PositionedWord> line) {
        String dateText = line.get(0).text();
        String tipo = line.get(1).text();

        Matcher dateMatcher = DATE_PATTERN.matcher(dateText);
        if (!dateMatcher.matches()) {
            throw new StatementParsingException("Linha central com data invalida: " + dateText);
        }
        LocalDate data = TransactionExtractor.parseDateDDMMYY(
                dateMatcher.group(1), dateMatcher.group(2), dateMatcher.group(3));

        StringBuilder inlineWords = new StringBuilder();
        for (int i = 2; i < line.size(); i++) {
            PositionedWord w = line.get(i);
            if (w.x0() >= INLINE_DESC_X_MIN && w.x0() < INLINE_DESC_X_MAX) {
                appendWord(inlineWords, w.text());
            }
        }

        StringBuilder valorWords = new StringBuilder();
        StringBuilder saldoWords = new StringBuilder();
        for (PositionedWord w : line) {
            if (w.x0() >= VALOR_X_MIN && w.x0() < VALOR_X_MAX) {
                appendWord(valorWords, w.text());
            } else if (w.x0() >= SALDO_X_MIN) {
                appendWord(saldoWords, w.text());
            }
        }

        // Saldo pode faltar (a coluna e opcional no schema), valor nao: uma
        // transacao sem valor legivel significa que o layout mudou, e seguir
        // adiante gravaria um extrato incompleto sem ninguem perceber.
        BigDecimal valor = TransactionExtractor.parseMoney(valorWords.toString());
        if (valor == null) {
            throw new StatementParsingException(
                    "Valor ilegivel na transacao de " + dateText + ": \"" + lineText(line) + "\"");
        }
        BigDecimal saldo = TransactionExtractor.parseMoney(saldoWords.toString());
        if ("Saída".equals(tipo) && valor.signum() > 0) {
            valor = valor.negate();
        }

        return new CoreLineData(data, tipo, inlineWords.toString().trim(), valor, saldo);
    }

    private static void appendWord(StringBuilder builder, String word) {
        if (builder.length() > 0) {
            builder.append(' ');
        }
        builder.append(word);
    }

    private List<ParsedTransaction> extractTransactions(List<List<PositionedWord>> allLines) {
        List<Integer> coreIdx = new ArrayList<>();
        List<Integer> colHeaderIdx = new ArrayList<>();
        for (int i = 0; i < allLines.size(); i++) {
            List<PositionedWord> line = allLines.get(i);
            if (isCoreLine(line)) {
                coreIdx.add(i);
            }
            if (isColumnHeaderLine(line)) {
                colHeaderIdx.add(i);
            }
        }

        List<ParsedTransaction> result = new ArrayList<>();

        for (int pos = 0; pos < coreIdx.size(); pos++) {
            int k = coreIdx.get(pos);
            CoreLineData core = parseCoreLine(allLines.get(k));

            int prevK = pos > 0 ? coreIdx.get(pos - 1) : -1;
            int descStart = prevK >= 0 ? prevK + 2 : 0;

            int lastHeader = -1;
            for (int h : colHeaderIdx) {
                if (h < k) {
                    lastHeader = Math.max(lastHeader, h);
                }
            }
            descStart = Math.max(descStart, lastHeader + 1);

            List<String> descLines = new ArrayList<>();
            for (int j = descStart; j < k; j++) {
                List<PositionedWord> line = allLines.get(j);
                if (!isHeaderLine(line)) {
                    descLines.add(lineText(line));
                }
            }
            descLines = dedupeConsecutive(descLines);

            StringBuilder descricao = new StringBuilder(String.join(" ", descLines));
            if (!core.inlineText().isBlank()) {
                if (descricao.length() > 0) {
                    descricao.append(' ');
                }
                descricao.append(core.inlineText());
            }

            String detalhe = "";
            if (k + 1 < allLines.size()) {
                List<PositionedWord> next = allLines.get(k + 1);
                if (!isHeaderLine(next) && !isCoreLine(next)) {
                    detalhe = lineText(next);
                }
            }

            result.add(new ParsedTransaction(
                    core.data(), stringToTransactionType(core.tipo()), core.valor(), core.saldo(),
                    descricao.toString().trim(), detalhe.trim()
            ));
        }

        return result;
    }

    private static TransactionType stringToTransactionType(String text) {
        return "Entrada".equals(text) ? TransactionType.ENTRADA : TransactionType.SAIDA;
    }

    /** Remove linhas de descricao consecutivas identicas (artefato de renderizacao em negrito no PDF). */
    private static List<String> dedupeConsecutive(List<String> parts) {
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            if (out.isEmpty() || !out.get(out.size() - 1).equals(p)) {
                out.add(p);
            }
        }
        return out;
    }

    /**
     * Le documento (CNPJ/CPF) e data de emissao do cabecalho (primeira pagina),
     * usados apenas para identificar o extrato (nao entram como colunas de transacao).
     */
    private StatementMetadata extractAccountInfo(List<List<PositionedWord>> firstPageLines) {
        String documento = "";
        LocalDate emitidoEm = null;

        List<String> texts = firstPageLines.stream()
                .map(StoneParser::lineText)
                .collect(Collectors.toList());

        for (int i = 0; i < texts.size(); i++) {
            String t = texts.get(i);

            if (t.startsWith("Nome") && t.contains("Documento") && i + 1 < texts.size()) {
                List<PositionedWord> nextWords = firstPageLines.get(i + 1);
                documento = nextWords.stream()
                        .filter(w -> w.x0() >= DOCUMENTO_X_MIN)
                        .map(PositionedWord::text)
                        .collect(Collectors.joining(" "));
            }

            Matcher m = EMITIDO_EM_PATTERN.matcher(t);
            if (m.lookingAt()) {
                int dia = Integer.parseInt(m.group(1));
                Integer mes = TransactionExtractor.monthFromPortugueseName(m.group(2));
                int ano = Integer.parseInt(m.group(3));
                if (mes != null) {
                    emitidoEm = LocalDate.of(ano, mes, dia);
                }
            }
        }

        return new StatementMetadata(documento, emitidoEm);
    }
}
