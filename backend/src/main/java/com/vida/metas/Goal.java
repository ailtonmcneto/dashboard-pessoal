package com.vida.metas;

import com.vida.common.OwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Meta (ou plano). O progresso é calculado conforme {@link ProgressMode}, nunca guardado. */
@Entity
@Table(name = "goals")
public class Goal extends OwnedEntity {

    public enum Area { ESTUDOS, FINANCAS, SAUDE, CARREIRA, PESSOAL }

    public enum Status { ATIVA, CONCLUIDA, ABANDONADA }

    /** MANUAL: valor atual/alvo; MARCOS e TAREFAS: concluídos/total; FINANCEIRO: saldo da conta/alvo. */
    public enum ProgressMode { MANUAL, MARCOS, TAREFAS, FINANCEIRO }

    @Column(nullable = false)
    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Area area;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    private LocalDate startsOn;

    private LocalDate dueOn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProgressMode progressMode;

    private BigDecimal targetValue;

    private BigDecimal currentValue;

    private String unit;

    private UUID accountId;

    private LocalDate completedOn;

    protected Goal() {
    }

    Goal(UUID userId) {
        super(userId);
    }

    void update(String title, String description, Area area, LocalDate startsOn, LocalDate dueOn,
            ProgressMode progressMode, BigDecimal targetValue, BigDecimal currentValue, String unit, UUID accountId) {
        this.title = title;
        this.description = description;
        this.area = area;
        this.startsOn = startsOn;
        this.dueOn = dueOn;
        this.progressMode = progressMode;
        this.targetValue = targetValue;
        this.currentValue = currentValue;
        this.unit = unit;
        this.accountId = accountId;
    }

    void setStatus(Status status, LocalDate today) {
        if (status == Status.CONCLUIDA && this.status != Status.CONCLUIDA) {
            this.completedOn = today;
        } else if (status != Status.CONCLUIDA) {
            this.completedOn = null;
        }
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Area getArea() {
        return area;
    }

    public Status getStatus() {
        return status;
    }

    public LocalDate getStartsOn() {
        return startsOn;
    }

    public LocalDate getDueOn() {
        return dueOn;
    }

    public ProgressMode getProgressMode() {
        return progressMode;
    }

    public BigDecimal getTargetValue() {
        return targetValue;
    }

    public BigDecimal getCurrentValue() {
        return currentValue;
    }

    public String getUnit() {
        return unit;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public LocalDate getCompletedOn() {
        return completedOn;
    }
}
