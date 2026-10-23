package com.vida.academico;

import com.vida.common.OwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "absences")
public class Absence extends OwnedEntity {

    @Column(nullable = false, updatable = false)
    private UUID courseId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private short classesMissed;

    private String note;

    protected Absence() {
    }

    Absence(UUID userId, UUID courseId, LocalDate date, int classesMissed, String note) {
        super(userId);
        this.courseId = courseId;
        this.date = date;
        this.classesMissed = (short) classesMissed;
        this.note = note;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public LocalDate getDate() {
        return date;
    }

    public int getClassesMissed() {
        return classesMissed;
    }

    public String getNote() {
        return note;
    }
}
