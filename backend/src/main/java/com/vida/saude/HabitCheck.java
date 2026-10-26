package com.vida.saude;

import com.vida.common.OwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

/** Um dia em que o hábito foi cumprido. */
@Entity
@Table(name = "habit_checks")
public class HabitCheck extends OwnedEntity {

    @Column(nullable = false, updatable = false)
    private UUID habitId;

    @Column(nullable = false, updatable = false)
    private LocalDate date;

    protected HabitCheck() {
    }

    HabitCheck(UUID userId, UUID habitId, LocalDate date) {
        super(userId);
        this.habitId = habitId;
        this.date = date;
    }

    public UUID getHabitId() {
        return habitId;
    }

    public LocalDate getDate() {
        return date;
    }
}
