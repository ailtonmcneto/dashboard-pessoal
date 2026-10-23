package com.vida.financas;

import com.vida.common.OwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Lançamento. O formato de cada tipo é garantido pelo banco (CHECKs da V4):
 * RECEITA (conta), DESPESA (conta ou cartão), TRANSFERENCIA (conta → conta),
 * PAGAMENTO_FATURA (conta paga a fatura de um cartão).
 */
@Entity
@Table(name = "transactions")
public class Transaction extends OwnedEntity {

    public enum Kind { RECEITA, DESPESA, TRANSFERENCIA, PAGAMENTO_FATURA }

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Kind kind;

    private UUID categoryId;

    private UUID accountId;

    private UUID cardId;

    private UUID toAccountId;

    private LocalDate invoiceMonth;

    @Column(nullable = false)
    private boolean paid;

    @Column(updatable = false)
    private UUID installmentGroupId;

    @Column(updatable = false)
    private Short installmentNo;

    @Column(updatable = false)
    private Short installmentTotal;

    @Column(updatable = false)
    private UUID recurrenceId;

    private String notes;

    protected Transaction() {
    }

    Transaction(UUID userId, Kind kind) {
        super(userId);
        this.kind = kind;
    }

    void setDetails(LocalDate date, String description, BigDecimal amount, UUID categoryId, String notes, boolean paid) {
        this.date = date;
        this.description = description;
        this.amount = amount;
        this.categoryId = categoryId;
        this.notes = notes;
        this.paid = paid;
    }

    void setPlace(UUID accountId, UUID cardId, UUID toAccountId, LocalDate invoiceMonth) {
        this.accountId = accountId;
        this.cardId = cardId;
        this.toAccountId = toAccountId;
        this.invoiceMonth = invoiceMonth;
    }

    void setInstallment(UUID groupId, int no, int total) {
        this.installmentGroupId = groupId;
        this.installmentNo = (short) no;
        this.installmentTotal = (short) total;
    }

    void setRecurrenceId(UUID recurrenceId) {
        this.recurrenceId = recurrenceId;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Kind getKind() {
        return kind;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public UUID getCardId() {
        return cardId;
    }

    public UUID getToAccountId() {
        return toAccountId;
    }

    public LocalDate getInvoiceMonth() {
        return invoiceMonth;
    }

    public boolean isPaid() {
        return paid;
    }

    public UUID getInstallmentGroupId() {
        return installmentGroupId;
    }

    public Short getInstallmentNo() {
        return installmentNo;
    }

    public Short getInstallmentTotal() {
        return installmentTotal;
    }

    public UUID getRecurrenceId() {
        return recurrenceId;
    }

    public String getNotes() {
        return notes;
    }
}
