package com.bankparser.repository;

import com.bankparser.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientRepository extends JpaRepository<Client, UUID> {

    List<Client> findByOrganizationIdOrderByNameAsc(UUID organizationId);

    Optional<Client> findByOrganizationIdAndId(UUID organizationId, UUID id);

    Optional<Client> findByOrganizationIdAndDocument(UUID organizationId, String document);

    boolean existsByOrganizationIdAndDocument(UUID organizationId, String document);
}
