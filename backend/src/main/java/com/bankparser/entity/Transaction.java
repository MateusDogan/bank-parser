package com.bankparser.entity;

import com.bankparser.parser.dto.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Uma linha extraida de um extrato, persistida a partir de
 * {@code ParsedTransaction}.
 */
@Entity
@Table(name = "transactions")
@SQLDelete(sql = "UPDATE transactions SET deleted = true, updated_at = now() WHERE id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class Transaction extends BaseEntity {

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "statement_id", nullable = false)
    private Statement statement;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    /** ENTRADA ou SAIDA, persistido como enum name (sem acentuacao). */
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TransactionType type;

    /** Positivo para Entrada, negativo para Saida. */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(precision = 15, scale = 2)
    private BigDecimal balance;

    @Column(columnDefinition = "text")
    private String description;

    @Column(columnDefinition = "text")
    private String detail;

    /**
     * Posicao da linha no extrato original. Varias transacoes compartilham a
     * mesma data, entao so a data nao reconstroi a ordem do PDF.
     */
    @Column(name = "line_number", nullable = false)
    private int lineNumber;

    public Transaction(Statement statement, int lineNumber) {
        this.statement = statement;
        this.organizationId = statement.getOrganizationId();
        this.lineNumber = lineNumber;
    }
}
