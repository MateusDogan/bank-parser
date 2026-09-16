package com.bankparser.repository;

import com.bankparser.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Page<Transaction> findByOrganizationIdAndStatementId(UUID organizationId, UUID statementId, Pageable pageable);

    /** Ordem original do PDF — usada na exportacao de CSV/Excel. */
    List<Transaction> findByOrganizationIdAndStatementIdOrderByLineNumber(UUID organizationId, UUID statementId);
}
