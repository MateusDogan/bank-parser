package com.bankparser.dto;

import com.bankparser.entity.Statement;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Contrato de API do extrato — separado da entidade JPA de proposito, para que
 * mudanca de schema nao quebre quem consome a API.
 */
public record StatementResponse(
        UUID id,
        UUID clientId,
        String clientName,
        String bankKey,
        String originalFilename,
        LocalDate issuedAt,
        String document,
        int transactionCount,
        Instant uploadedAt,
        String parserVersion,
        String validationFlags
) {
    public static StatementResponse from(Statement statement) {
        return new StatementResponse(
                statement.getId(),
                statement.getClient().getId(),
                statement.getClient().getName(),
                statement.getBankKey(),
                statement.getOriginalFilename(),
                statement.getIssuedAt(),
                statement.getDocument(),
                statement.getTransactionCount(),
                statement.getUploadedAt(),
                statement.getParserVersion(),
                statement.getValidationFlags());
    }
}
