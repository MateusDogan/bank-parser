package com.bankparser.repository;

import com.bankparser.entity.Statement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StatementRepository extends JpaRepository<Statement, UUID> {

    /** Mais recente primeiro, atendido pelo indice idx_statements_uploaded. */
    List<Statement> findAllByOrderByUploadedAtDesc();
}
