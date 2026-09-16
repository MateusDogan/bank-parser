package com.bankparser.repository;

import com.bankparser.entity.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClientRepository extends JpaRepository<Client, UUID> {

    Page<Client> findByOrganizationId(UUID organizationId, Pageable pageable);

    Optional<Client> findByOrganizationIdAndId(UUID organizationId, UUID id);

    Optional<Client> findByOrganizationIdAndDocument(UUID organizationId, String document);

    boolean existsByOrganizationIdAndDocument(UUID organizationId, String document);
}
