package com.bankparser.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
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
 *
 * <p>Esse id sempre preenchido tem um efeito colateral caro: o Spring Data
 * decide entre {@code persist} e {@code merge} perguntando se o id e nulo,
 * entao todo {@code save()} caia no {@code merge} — e cada merge dispara um
 * SELECT antes do INSERT para procurar a linha que ainda nao existe. Num
 * extrato de 264 transacoes, sao 264 consultas jogadas fora. Implementar
 * {@link Persistable} deixa a entidade responder isso por conta propria,
 * mantendo o id pre-gerado.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseEntity implements Persistable<UUID> {

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

    /** Sem getter/setter publicos de proposito: mexer nisso de fora gera insert duplicado. */
    @Transient
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private boolean persisted;

    @Override
    public boolean isNew() {
        return !persisted;
    }

    /**
     * {@code @PrePersist} e nao {@code @PostPersist}: o callback precisa rodar
     * dentro do {@code persist()}, nao no flush. {@code SimpleJpaRepository
     * .delete()} ignora entidades que ainda se declaram novas, entao um
     * {@code save()} seguido de {@code delete()} na mesma transacao viraria
     * um no-op silencioso se a marcacao esperasse o flush.
     */
    @PrePersist
    @PostLoad
    void markPersisted() {
        this.persisted = true;
    }
}
