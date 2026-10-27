package com.vida.academico;

import com.vida.common.OwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Semestre (ou período letivo). */
@Entity
@Table(name = "terms")
public class Term extends OwnedEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate startsOn;

    @Column(nullable = false)
    private LocalDate endsOn;

    @Column(nullable = false)
    private BigDecimal passingGrade;

    protected Term() {
    }

    Term(UUID userId) {
        super(userId);
    }

    void update(String name, LocalDate startsOn, LocalDate endsOn, BigDecimal passingGrade) {
        this.name = name;
        this.startsOn = startsOn;
        this.endsOn = endsOn;
        this.passingGrade = passingGrade;
    }

    public boolean contains(LocalDate day) {
        return !day.isBefore(startsOn) && !day.isAfter(endsOn);
    }

    public String getName() {
        return name;
    }

    public LocalDate getStartsOn() {
        return startsOn;
    }

    public LocalDate getEndsOn() {
        return endsOn;
    }

    public BigDecimal getPassingGrade() {
        return passingGrade;
    }
}
