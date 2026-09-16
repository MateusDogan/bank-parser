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
 * Funcionario do escritorio. Sem credenciais por enquanto: a autenticacao
 * entra na Fase 7 e adiciona os campos de senha/role sobre esta entidade.
 */
@Entity
@Table(name = "users")
@SQLDelete(sql = "UPDATE users SET deleted = true, updated_at = now() WHERE id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class User extends BaseEntity {

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 255)
    private String email;

    public User(UUID organizationId, String name, String email) {
        this.organizationId = organizationId;
        this.name = name;
        this.email = email;
    }
}
