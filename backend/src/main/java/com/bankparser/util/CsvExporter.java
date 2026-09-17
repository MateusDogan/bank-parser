package com.bankparser.util;

import com.bankparser.parser.BankStatementParser;
import com.bankparser.parser.StoneParser;
import com.bankparser.parser.dto.ParsedTransaction;
import com.bankparser.parser.dto.ParsingResult;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Formato CSV do extrato — separador ";", data dd/MM/yyyy, valores com 2 casas.
 * Mudar aqui muda o que o escritório já consome, então conferir antes.
 */
public final class CsvExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String HEADER = "data;tipo;valor;saldo;descricao;detalhe";

    private CsvExporter() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Uso: java CsvExporter <caminho_pdf_entrada> <caminho_csv_saida>");
            System.exit(1);
        }

        String pdfPath = args[0];
        String csvPath = args[1];

        try (FileInputStream pdfStream = new FileInputStream(pdfPath)) {
            BankStatementParser parser = new StoneParser();
            ParsingResult result = parser.parse(pdfStream);

            exportToCsv(result, csvPath);
            System.out.println("CSV exportado: " + csvPath);
            System.out.println("  Transacoes: " + result.transactions().size());
            System.out.println("  Documento: " + result.metadata().documento());
            System.out.println("  Emitido em: " + result.metadata().emitidoEm());
        }
    }

    public static void exportToCsv(ParsingResult result, String csvPath) throws IOException {
        Path path = Paths.get(csvPath);
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (OutputStream out = Files.newOutputStream(path)) {
            writeCsv(result.transactions(), out);
        }
    }

    /**
     * Escreve o CSV direto no stream, sem passar por arquivo — e assim que o
     * endpoint de export responde ao HTTP.
     *
     * <p>Nao fecha {@code out}: quem o abriu decide o ciclo de vida dele (o
     * container fecha o stream da resposta HTTP).
     */
    public static void writeCsv(List<ParsedTransaction> transactions, OutputStream out) throws IOException {
        Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        writer.write(HEADER);
        writer.write('\n');

        for (ParsedTransaction tx : transactions) {
            writer.write(String.format("%s;%s;%s;%s;%s;%s\n",
                    tx.data().format(DATE_FORMATTER),
                    tx.tipo(),
                    formatNumber(tx.valor()),
                    formatNumber(tx.saldo()),
                    sanitizeCsvField(tx.descricao()),
                    sanitizeCsvField(tx.detalhe())));
        }
        writer.flush();
    }

    private static String formatNumber(BigDecimal value) {
        // Padrao contabil: sempre 2 casas decimais (3000.00, 0.01).
        return value == null ? "" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String sanitizeCsvField(String field) {
        return field == null ? "" : field.replace("\"", "\"\"");
    }
}
