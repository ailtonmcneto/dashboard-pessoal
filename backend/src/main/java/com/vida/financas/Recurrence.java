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

/** Molde de lançamento que se repete (aluguel, salário, assinatura…). */
@Entity
@Table(name = "recurrences")
public class Recurrence extends OwnedEntity {

    public enum Frequency { SEMANAL, MENSAL, ANUAL }

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Transaction.Kind kind;

    private UUID categoryId;

    private UUID accountId;

    private UUID cardId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Frequency frequency;

    @Column(nullable = false)
    private LocalDate startsOn;

    private LocalDate endsOn;

    /** Ocorrências já geradas; a próxima é a de índice {@code generated}. */
    @Column(nullable = false)
    private int generated;

    @Column(nullable = false)
    private LocalDate nextOn;

    @Column(nullable = false)
    private boolean active;

    protected Recurrence() {
    }

    Recurrence(UUID userId) {
        super(userId);
    }

    /**
     * Na criação, gera desde {@code startsOn} (inclusive datas passadas). Numa edição que muda a
     * data ou a frequência, recomeça depois de hoje, para não duplicar o que já foi lançado.
     */
    void update(String description, BigDecimal amount, Transaction.Kind kind, UUID categoryId, UUID accountId,
            UUID cardId, Frequency frequency, LocalDate startsOn, LocalDate endsOn, LocalDate today) {
        boolean isNew = this.startsOn == null;
        boolean scheduleChanged = !isNew && (!this.startsOn.equals(startsOn) || this.frequency != frequency);
        this.description = description;
        this.amount = amount;
        this.kind = kind;
        this.categoryId = categoryId;
        this.accountId = accountId;
        this.cardId = cardId;
        this.frequency = frequency;
        this.startsOn = startsOn;
        this.endsOn = endsOn;
        if (isNew) {
            this.generated = 0;
        } else if (scheduleChanged) {
            int n = 0;
            while (!Schedules.occurrence(startsOn, frequency, n).isAfter(today)) {
                n++;
            }
            this.generated = n;
        }
        this.nextOn = Schedules.occurrence(startsOn, frequency, generated);
        this.active = endsOn == null || !nextOn.isAfter(endsOn);
    }

    /** Marca a próxima ocorrência como gerada e avança. */
    void advance() {
        generated++;
        nextOn = Schedules.occurrence(startsOn, frequency, generated);
        if (endsOn != null && nextOn.isAfter(endsOn)) {
            active = false;
        }
    }

    void setActive(boolean active) {
        this.active = active;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Transaction.Kind getKind() {
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

    public Frequency getFrequency() {
        return frequency;
    }

    public LocalDate getStartsOn() {
        return startsOn;
    }

    public LocalDate getEndsOn() {
        return endsOn;
    }

    public int getGenerated() {
        return generated;
    }

    public LocalDate getNextOn() {
        return nextOn;
    }

    public boolean isActive() {
        return active;
    }
}
