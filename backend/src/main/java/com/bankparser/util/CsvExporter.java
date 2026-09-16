package com.bankparser.util;

import com.bankparser.parser.BankStatementParser;
import com.bankparser.parser.StoneParser;
import com.bankparser.parser.dto.ParsedTransaction;
import com.bankparser.parser.dto.ParsingResult;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CsvExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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
            System.out.println("✓ CSV exportado com sucesso: " + csvPath);
            System.out.println("  Transações: " + result.transactions().size());
            System.out.println("  Documento: " + result.metadata().documento());
            System.out.println("  Emitido em: " + result.metadata().emitidoEm());
        }
    }

    public static void exportToCsv(ParsingResult result, String csvPath) throws IOException {
        Path path = Paths.get(csvPath);
        Files.createDirectories(path.getParent());

        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(csvPath), StandardCharsets.UTF_8)) {

            writer.write("data;tipo;valor;saldo;descricao;detalhe\n");

            List<ParsedTransaction> transactions = result.transactions();
            for (ParsedTransaction tx : transactions) {
                String line = String.format("%s;%s;%s;%s;%s;%s\n",
                        tx.data().format(DATE_FORMATTER),
                        tx.tipo(),
                        formatNumber(tx.valor()),
                        formatNumber(tx.saldo()),
                        sanitizeCsvField(tx.descricao()),
                        sanitizeCsvField(tx.detalhe())
                );
                writer.write(line);
            }
        }
    }

    private static String formatNumber(java.math.BigDecimal value) {
        if (value == null) return "";
        return value.stripTrailingZeros().toPlainString();
    }

    private static String sanitizeCsvField(String field) {
        if (field == null) return "";
        return field.replace("\"", "\"\"");
    }
}
