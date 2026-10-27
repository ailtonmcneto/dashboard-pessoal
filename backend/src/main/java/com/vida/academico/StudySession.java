package com.vida.academico;

import com.vida.common.OwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Bloco de estudo registrado: quando, quanto e (opcionalmente) de quê. */
@Entity
@Table(name = "study_sessions")
public class StudySession extends OwnedEntity {

    @Column(nullable = false)
    private Instant startedAt;

    @Column(nullable = false)
    private int minutes;

    private UUID courseId;

    private UUID resourceId;

    private UUID topicId;

    private String notes;

    protected StudySession() {
    }

    StudySession(UUID userId, Instant startedAt, int minutes, UUID courseId, UUID resourceId, UUID topicId,
            String notes) {
        super(userId);
        this.startedAt = startedAt;
        this.minutes = minutes;
        this.courseId = courseId;
        this.resourceId = resourceId;
        this.topicId = topicId;
        this.notes = notes;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public int getMinutes() {
        return minutes;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public UUID getTopicId() {
        return topicId;
    }

    public String getNotes() {
        return notes;
    }
}
