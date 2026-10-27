package com.vida.saude;

import com.vida.common.OwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "habits")
public class Habit extends OwnedEntity {

    /** Diário; só em dias da semana escolhidos; ou X vezes por semana, em qualquer dia. */
    public enum Schedule { DIARIO, DIAS_SEMANA, X_POR_SEMANA }

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Schedule schedule;

    /** Bit (dia ISO − 1): seg = 1, ter = 2, qua = 4 … dom = 64. */
    private Short weekdaysMask;

    private Short timesPerWeek;

    @Column(nullable = false)
    private LocalDate startsOn;

    @Column(nullable = false)
    private String color;

    @Column(nullable = false)
    private boolean archived;

    protected Habit() {
    }

    Habit(UUID userId) {
        super(userId);
    }

    void update(String name, Schedule schedule, Set<DayOfWeek> weekdays, Integer timesPerWeek, LocalDate startsOn,
            String color, boolean archived) {
        this.name = name;
        this.schedule = schedule;
        this.weekdaysMask = schedule == Schedule.DIAS_SEMANA ? HabitStats.mask(weekdays) : null;
        this.timesPerWeek = schedule == Schedule.X_POR_SEMANA ? timesPerWeek.shortValue() : null;
        this.startsOn = startsOn;
        this.color = color;
        this.archived = archived;
    }

    HabitStats.Rule rule() {
        return new HabitStats.Rule(schedule, weekdaysMask == null ? 0 : weekdaysMask,
                timesPerWeek == null ? 0 : timesPerWeek, startsOn);
    }

    public String getName() {
        return name;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public Short getWeekdaysMask() {
        return weekdaysMask;
    }

    public Short getTimesPerWeek() {
        return timesPerWeek;
    }

    public LocalDate getStartsOn() {
        return startsOn;
    }

    public String getColor() {
        return color;
    }

    public boolean isArchived() {
        return archived;
    }
}
