package com.bankparser.repository;

import com.bankparser.entity.Statement;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StatementRepository extends JpaRepository<Statement, UUID> {

    // O client vem junto: a resposta da API expõe o nome dele e é montada fora
    // da transação (open-in-view está desligado), então lazy aqui quebraria.
    //
    // Mais recente primeiro, atendido pelo indice idx_statements_org_uploaded.
    @EntityGraph(attributePaths = "client")
    List<Statement> findByOrganizationIdOrderByUploadedAtDesc(UUID organizationId);

    @EntityGraph(attributePaths = "client")
    Optional<Statement> findByOrganizationIdAndId(UUID organizationId, UUID id);
}
