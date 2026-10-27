package com.vida.academico;

import com.vida.common.OwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

/** Aula fixa no horário semanal. */
@Entity
@Table(name = "class_slots")
public class ClassSlot extends OwnedEntity {

    @Column(nullable = false, updatable = false)
    private UUID courseId;

    /** ISO: 1 = segunda … 7 = domingo. */
    @Column(nullable = false)
    private short weekday;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    private String room;

    protected ClassSlot() {
    }

    ClassSlot(UUID userId, UUID courseId, DayOfWeek weekday, LocalTime startTime, LocalTime endTime, String room) {
        super(userId);
        this.courseId = courseId;
        this.weekday = (short) weekday.getValue();
        this.startTime = startTime;
        this.endTime = endTime;
        this.room = room;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public DayOfWeek getWeekday() {
        return DayOfWeek.of(weekday);
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public String getRoom() {
        return room;
    }
}
