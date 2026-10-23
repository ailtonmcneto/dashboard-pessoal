package com.vida.financas;

import com.vida.common.OwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

/** Categoria de receita ou despesa; aceita um nível de subcategoria. */
@Entity
@Table(name = "categories")
public class Category extends OwnedEntity {

    public enum Kind { RECEITA, DESPESA }

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Kind kind;

    private UUID parentId;

    @Column(nullable = false)
    private String color;

    @Column(nullable = false)
    private boolean archived;

    protected Category() {
    }

    Category(UUID userId, Kind kind) {
        super(userId);
        this.kind = kind;
    }

    void update(String name, UUID parentId, String color, boolean archived) {
        this.name = name;
        this.parentId = parentId;
        this.color = color;
        this.archived = archived;
    }

    public String getName() {
        return name;
    }

    public Kind getKind() {
        return kind;
    }

    public UUID getParentId() {
        return parentId;
    }

    public String getColor() {
        return color;
    }

    public boolean isArchived() {
        return archived;
    }
}
