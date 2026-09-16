package com.bankparser.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

/**
 * CNPJ/CPF atendido pelo escritorio. Os extratos processados pertencem sempre
 * a um Client.
 */
@Entity
@Table(name = "clients")
@SQLDelete(sql = "UPDATE clients SET deleted = true, updated_at = now() WHERE id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class Client extends BaseEntity {

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false, length = 255)
    private String name;

    /** Somente digitos, sem mascara — ver {@link #normalizeDocument(String)}. */
    @Column(nullable = false, length = 14)
    private String document;

    public Client(UUID organizationId, String name, String document) {
        this.organizationId = organizationId;
        this.name = name;
        this.document = normalizeDocument(document);
    }

    public void setDocument(String document) {
        this.document = normalizeDocument(document);
    }

    /**
     * O mesmo CNPJ aparece com mascaras diferentes conforme a origem (digitado
     * a mao, lido do cabecalho do PDF), entao a forma canonica e so os digitos.
     */
    public static String normalizeDocument(String document) {
        return document == null ? null : document.replaceAll("\\D", "");
    }
}
