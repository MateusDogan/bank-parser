package com.bankparser.controller;

import com.bankparser.dto.StatementResponse;
import com.bankparser.entity.Statement;
import com.bankparser.parser.dto.ParsedTransaction;
import com.bankparser.service.CurrentOrganizationProvider;
import com.bankparser.service.StatementProcessingService;
import com.bankparser.util.CsvExporter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/statements")
public class StatementController {

    private static final MediaType TEXT_CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final StatementProcessingService processingService;
    private final CurrentOrganizationProvider organizationProvider;

    public StatementController(StatementProcessingService processingService,
                               CurrentOrganizationProvider organizationProvider) {
        this.processingService = processingService;
        this.organizationProvider = organizationProvider;
    }

    @PostMapping("/upload")
    public ResponseEntity<StatementResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("clientId") UUID clientId,
            @RequestParam(name = "bankKey", defaultValue = "stone") String bankKey) throws IOException {

        Statement statement = processingService.process(
                organizationProvider.currentOrganizationId(),
                clientId,
                bankKey,
                file.getOriginalFilename(),
                file.getBytes());

        return ResponseEntity.status(HttpStatus.CREATED).body(StatementResponse.from(statement));
    }

    @GetMapping("/{id}")
    public StatementResponse findById(@PathVariable UUID id) {
        return StatementResponse.from(
                processingService.findById(organizationProvider.currentOrganizationId(), id));
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<Resource> export(@PathVariable UUID id,
                                           @RequestParam(defaultValue = "csv") String format) throws IOException {
        UUID organizationId = organizationProvider.currentOrganizationId();
        // Excel entra na Fase 4/5, junto dos relatorios.
        if (!"csv".equalsIgnoreCase(format)) {
            return ResponseEntity.badRequest().build();
        }

        Statement statement = processingService.findById(organizationId, id);
        List<ParsedTransaction> transactions = processingService.transactionsForExport(organizationId, id);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        CsvExporter.writeCsv(transactions, buffer);

        return ResponseEntity.ok()
                .contentType(TEXT_CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(csvFilename(statement))
                        .build()
                        .toString())
                .body(new ByteArrayResource(buffer.toByteArray()));
    }

    private static String csvFilename(Statement statement) {
        String base = statement.getOriginalFilename();
        if (base != null && base.toLowerCase().endsWith(".pdf")) {
            base = base.substring(0, base.length() - 4);
        }
        return (base == null || base.isBlank() ? "extrato" : base) + ".csv";
    }
}
