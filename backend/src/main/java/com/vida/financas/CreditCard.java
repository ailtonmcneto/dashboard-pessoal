package com.vida.financas;

import com.vida.common.OwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "credit_cards")
public class CreditCard extends OwnedEntity {

    @Column(nullable = false)
    private String name;

    private BigDecimal creditLimit;

    /** Dia em que a fatura fecha (1–28). Compras a partir desse dia vão para a fatura seguinte. */
    @Column(nullable = false)
    private short closingDay;

    /** Dia do vencimento (1–28). */
    @Column(nullable = false)
    private short dueDay;

    private UUID paymentAccountId;

    @Column(nullable = false)
    private boolean archived;

    protected CreditCard() {
    }

    CreditCard(UUID userId) {
        super(userId);
    }

    void update(String name, BigDecimal creditLimit, int closingDay, int dueDay, UUID paymentAccountId,
            boolean archived) {
        this.name = name;
        this.creditLimit = creditLimit;
        this.closingDay = (short) closingDay;
        this.dueDay = (short) dueDay;
        this.paymentAccountId = paymentAccountId;
        this.archived = archived;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public int getClosingDay() {
        return closingDay;
    }

    public int getDueDay() {
        return dueDay;
    }

    public UUID getPaymentAccountId() {
        return paymentAccountId;
    }

    public boolean isArchived() {
        return archived;
    }
}
