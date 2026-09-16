package com.bankparser.dto;

import com.bankparser.entity.Client;

import java.time.Instant;
import java.util.UUID;

/**
 * Contrato de API do cliente — separado da entidade JPA de proposito, para que
 * mudanca de schema nao quebre quem consome a API.
 */
public record ClientResponse(
        UUID id,
        String name,
        String document,
        Instant createdAt
) {
    public static ClientResponse from(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getName(),
                client.getDocument(),
                client.getCreatedAt());
    }
}
