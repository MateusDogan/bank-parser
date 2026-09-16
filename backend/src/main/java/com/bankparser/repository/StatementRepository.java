package com.bankparser.repository;

import com.bankparser.entity.Statement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StatementRepository extends JpaRepository<Statement, UUID> {

    // O client vem junto: a resposta da API expõe o nome dele e é montada fora
    // da transação (open-in-view está desligado), então lazy aqui quebraria.
    @EntityGraph(attributePaths = "client")
    Page<Statement> findByOrganizationId(UUID organizationId, Pageable pageable);

    @EntityGraph(attributePaths = "client")
    Page<Statement> findByOrganizationIdAndClientId(UUID organizationId, UUID clientId, Pageable pageable);

    @EntityGraph(attributePaths = "client")
    Optional<Statement> findByOrganizationIdAndId(UUID organizationId, UUID id);
}
