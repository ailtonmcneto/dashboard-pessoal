package com.vida.metas;

import com.vida.common.OwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Tarefa da lista única; os vínculos (meta, marco, projeto, disciplina) são todos opcionais. */
@Entity
@Table(name = "tasks")
public class Task extends OwnedEntity {

    @Column(nullable = false)
    private String title;

    private String notes;

    /** 1 alta, 2 média, 3 baixa. */
    @Column(nullable = false)
    private short priority;

    private LocalDate dueOn;

    private Instant doneAt;

    private UUID goalId;

    private UUID milestoneId;

    private UUID projectId;

    private UUID courseId;

    protected Task() {
    }

    Task(UUID userId) {
        super(userId);
    }

    void update(String title, String notes, int priority, LocalDate dueOn, UUID goalId, UUID milestoneId,
            UUID projectId, UUID courseId) {
        this.title = title;
        this.notes = notes;
        this.priority = (short) priority;
        this.dueOn = dueOn;
        this.goalId = goalId;
        this.milestoneId = milestoneId;
        this.projectId = projectId;
        this.courseId = courseId;
    }

    void setDoneAt(Instant doneAt) {
        this.doneAt = doneAt;
    }

    public boolean isDone() {
        return doneAt != null;
    }

    public String getTitle() {
        return title;
    }

    public String getNotes() {
        return notes;
    }

    public int getPriority() {
        return priority;
    }

    public LocalDate getDueOn() {
        return dueOn;
    }

    public Instant getDoneAt() {
        return doneAt;
    }

    public UUID getGoalId() {
        return goalId;
    }

    public UUID getMilestoneId() {
        return milestoneId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getCourseId() {
        return courseId;
    }
}
