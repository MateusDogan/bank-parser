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
 * Tenant. Hoje existe apenas a organizacao padrao (o escritorio); a partir da
 * Fase 10 cada escritorio cliente do SaaS vira um registro aqui.
 */
@Entity
@Table(name = "organizations")
@SQLDelete(sql = "UPDATE organizations SET deleted = true, updated_at = now() WHERE id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class Organization extends BaseEntity {

    /** Organizacao criada pela migration V2, referenciada enquanto nao ha cadastro de tenants. */
    public static final UUID DEFAULT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Column(nullable = false, length = 160)
    private String name;

    public Organization(String name) {
        this.name = name;
    }
}
