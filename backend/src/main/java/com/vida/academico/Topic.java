package com.vida.academico;

import com.vida.common.OwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

/** Assunto de estudo, com domínio (0–5) e data da próxima revisão. */
@Entity
@Table(name = "topics")
public class Topic extends OwnedEntity {

    private UUID courseId;

    private UUID resourceId;

    private UUID parentId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private short mastery;

    private LocalDate lastReviewedOn;

    private LocalDate nextReviewOn;

    protected Topic() {
    }

    Topic(UUID userId) {
        super(userId);
    }

    void update(String title, UUID courseId, UUID resourceId, UUID parentId) {
        this.title = title;
        this.courseId = courseId;
        this.resourceId = resourceId;
        this.parentId = parentId;
    }

    void review(int mastery, LocalDate on, LocalDate next) {
        this.mastery = (short) mastery;
        this.lastReviewedOn = on;
        this.nextReviewOn = next;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public UUID getParentId() {
        return parentId;
    }

    public String getTitle() {
        return title;
    }

    public int getMastery() {
        return mastery;
    }

    public LocalDate getLastReviewedOn() {
        return lastReviewedOn;
    }

    public LocalDate getNextReviewOn() {
        return nextReviewOn;
    }
}
