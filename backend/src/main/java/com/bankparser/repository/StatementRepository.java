package com.bankparser.repository;

import com.bankparser.entity.Statement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StatementRepository extends JpaRepository<Statement, UUID> {

    Page<Statement> findByOrganizationId(UUID organizationId, Pageable pageable);

    Page<Statement> findByOrganizationIdAndClientId(UUID organizationId, UUID clientId, Pageable pageable);

    Optional<Statement> findByOrganizationIdAndId(UUID organizationId, UUID id);
}
