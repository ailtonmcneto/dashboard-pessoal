package com.vida.financas;

import com.vida.common.OwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Limite de gasto de uma categoria de despesa num mês. */
@Entity
@Table(name = "budgets")
public class Budget extends OwnedEntity {

    @Column(nullable = false, updatable = false)
    private UUID categoryId;

    /** Sempre o dia 1 do mês. */
    @Column(nullable = false, updatable = false)
    private LocalDate month;

    @Column(nullable = false)
    private BigDecimal limitAmount;

    protected Budget() {
    }

    Budget(UUID userId, UUID categoryId, LocalDate month) {
        super(userId);
        this.categoryId = categoryId;
        this.month = month;
    }

    void setLimitAmount(BigDecimal limitAmount) {
        this.limitAmount = limitAmount;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public LocalDate getMonth() {
        return month;
    }

    public BigDecimal getLimitAmount() {
        return limitAmount;
    }
}
