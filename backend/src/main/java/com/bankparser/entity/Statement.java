package com.bankparser.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Um PDF de extrato enviado e processado.
 *
 * <p>Reenviar o mesmo arquivo cria um novo Statement: o sistema nao deduplica
 * por conteudo (decisao explicita — quem envia responde pelo reenvio).
 */
@Entity
@Table(name = "statements")
@SQLDelete(sql = "UPDATE statements SET deleted = true, updated_at = now() WHERE id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class Statement extends BaseEntity {

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    /** Qual {@code BankStatementParser} processou o arquivo (ex.: "stone"). */
    @Column(name = "bank_key", nullable = false, length = 40)
    private String bankKey;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    /** Chave do PDF no storage. Preenchida na Fase 3, quando o MinIO entra. */
    @Column(name = "storage_key", length = 512)
    private String storageKey;

    /** Data de emissao lida do cabecalho; nula se o layout do PDF nao trouxer. */
    @Column(name = "issued_at")
    private LocalDate issuedAt;

    /** CNPJ/CPF como veio no cabecalho do PDF, para conferir contra o Client. */
    @Column(length = 32)
    private String document;

    @Column(name = "transaction_count", nullable = false)
    private int transactionCount;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt = Instant.now();

    /** Versao do parser que processou este Statement (ex.: "1.0"), para auditoria. */
    @Column(name = "parser_version", length = 10)
    private String parserVersion;

    /** Resultado da checagem de continuidade de saldo (resumo textual, null se OK). */
    @Column(name = "validation_flags", columnDefinition = "text")
    private String validationFlags;

    public Statement(UUID organizationId, Client client, String bankKey, String originalFilename) {
        this.organizationId = organizationId;
        this.client = client;
        this.bankKey = bankKey;
        this.originalFilename = originalFilename;
    }
}
