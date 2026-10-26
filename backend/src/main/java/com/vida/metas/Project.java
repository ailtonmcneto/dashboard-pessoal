package com.vida.metas;

import com.vida.common.OwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

/** Projeto de estudo, pessoal ou de trabalho; pode servir a uma meta e/ou a uma disciplina. */
@Entity
@Table(name = "projects")
public class Project extends OwnedEntity {

    public enum Kind { ESTUDO, PESSOAL, TRABALHO }

    public enum Status { PLANEJADO, ATIVO, PAUSADO, CONCLUIDO }

    @Column(nullable = false)
    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Kind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    private LocalDate startsOn;

    private LocalDate dueOn;

    private String url;

    private UUID goalId;

    private UUID courseId;

    protected Project() {
    }

    Project(UUID userId) {
        super(userId);
    }

    void update(String title, String description, Kind kind, Status status, LocalDate startsOn, LocalDate dueOn,
            String url, UUID goalId, UUID courseId) {
        this.title = title;
        this.description = description;
        this.kind = kind;
        this.status = status;
        this.startsOn = startsOn;
        this.dueOn = dueOn;
        this.url = url;
        this.goalId = goalId;
        this.courseId = courseId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Kind getKind() {
        return kind;
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

    public String getUrl() {
        return url;
    }

    public UUID getGoalId() {
        return goalId;
    }

    public UUID getCourseId() {
        return courseId;
    }
}
