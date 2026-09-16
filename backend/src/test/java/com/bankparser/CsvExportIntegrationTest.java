package com.bankparser;

import com.bankparser.parser.BankStatementParser;
import com.bankparser.parser.StoneParser;
import com.bankparser.parser.dto.ParsedTransaction;
import com.bankparser.parser.dto.ParsingResult;
import org.junit.jupiter.api.Test;

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

public class CsvExportIntegrationTest {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String PDF_PATH = "C:\\Users\\User\\Workspace\\Projetos\\Parser\\raw_pdfs\\extrato-EC6BB48B-6EB8-49E2-B1D2-EBFF246F8FA7.pdf";
    private static final String CSV_OUT = "C:\\Users\\User\\Workspace\\Projetos\\Parser\\extracted_csv\\extrato-java-gerado.csv";

    @Test
    void exportPdfToCsv() throws IOException {
        try (FileInputStream pdfStream = new FileInputStream(PDF_PATH)) {
            BankStatementParser parser = new StoneParser();
            ParsingResult result = parser.parse(pdfStream);

            exportToCsv(result, CSV_OUT);

            System.out.println("\n✓ CSV exportado com sucesso: " + CSV_OUT);
            System.out.println("  Transações: " + result.transactions().size());
            System.out.println("  Documento: " + result.metadata().documento());
            System.out.println("  Emitido em: " + result.metadata().emitidoEm());
        }
    }

    private void exportToCsv(ParsingResult result, String csvPath) throws IOException {
        Path path = Paths.get(csvPath);
        Files.createDirectories(path.getParent());

        try (FileOutputStream fos = new FileOutputStream(csvPath);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {

            // Add UTF-8 BOM for Excel compatibility (matches Python behavior)
            writer.write('﻿');

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

    private String formatNumber(java.math.BigDecimal value) {
        if (value == null) return "";
        // Remove trailing zeros but keep significant decimals (e.g., 30000.00 -> 30000, 0.01 -> 0.01)
        return value.stripTrailingZeros().toPlainString();
    }

    private String sanitizeCsvField(String field) {
        if (field == null) return "";
        return field.replace("\"", "\"\"");
    }
}
