package com.vida.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;

/**
 * Entidade que pertence a um usuário. O dono é fixado na criação e nunca muda;
 * repositórios só buscam por {@code (id, userId)}.
 */
@MappedSuperclass
public abstract class OwnedEntity extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    protected OwnedEntity() {
    }

    protected OwnedEntity(UUID userId) {
        this.userId = userId;
    }

    public UUID getUserId() {
        return userId;
    }
}
