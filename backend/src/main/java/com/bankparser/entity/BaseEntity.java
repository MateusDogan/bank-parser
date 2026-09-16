package com.bankparser.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Campos comuns a todas as entidades: identidade, timestamps de auditoria e
 * a flag de soft-delete.
 *
 * <p>O id e gerado na aplicacao (e nao por sequence do banco) para que o
 * objeto ja tenha identidade antes de persistir — necessario para montar o
 * grafo Statement + Transactions em memoria antes do insert.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseEntity {

    @Id
    private UUID id = UUID.randomUUID();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private boolean deleted;
}
