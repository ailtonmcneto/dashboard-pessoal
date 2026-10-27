package com.vida.metas;

import com.vida.common.OwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

/** Etapa de uma meta. */
@Entity
@Table(name = "milestones")
public class Milestone extends OwnedEntity {

    @Column(nullable = false, updatable = false)
    private UUID goalId;

    @Column(nullable = false)
    private String title;

    private LocalDate dueOn;

    private LocalDate doneOn;

    @Column(nullable = false)
    private int position;

    protected Milestone() {
    }

    Milestone(UUID userId, UUID goalId, int position) {
        super(userId);
        this.goalId = goalId;
        this.position = position;
    }

    void update(String title, LocalDate dueOn) {
        this.title = title;
        this.dueOn = dueOn;
    }

    void setDoneOn(LocalDate doneOn) {
        this.doneOn = doneOn;
    }

    public UUID getGoalId() {
        return goalId;
    }

    public String getTitle() {
        return title;
    }

    public LocalDate getDueOn() {
        return dueOn;
    }

    public LocalDate getDoneOn() {
        return doneOn;
    }

    public int getPosition() {
        return position;
    }
}
