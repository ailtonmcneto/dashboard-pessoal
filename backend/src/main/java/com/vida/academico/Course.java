package com.vida.academico;

import com.vida.common.OwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

/** Disciplina de um semestre. */
@Entity
@Table(name = "courses")
public class Course extends OwnedEntity {

    public enum Status { CURSANDO, APROVADO, REPROVADO, TRANCADO }

    @Column(nullable = false)
    private UUID termId;

    @Column(nullable = false)
    private String name;

    private String code;

    private String professor;

    private Integer workloadHours;

    private Integer maxAbsences;

    @Column(nullable = false)
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    protected Course() {
    }

    Course(UUID userId, UUID termId) {
        super(userId);
        this.termId = termId;
    }

    void update(UUID termId, String name, String code, String professor, Integer workloadHours, Integer maxAbsences,
            String color, Status status) {
        this.termId = termId;
        this.name = name;
        this.code = code;
        this.professor = professor;
        this.workloadHours = workloadHours;
        this.maxAbsences = maxAbsences;
        this.color = color;
        this.status = status;
    }

    public UUID getTermId() {
        return termId;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public String getProfessor() {
        return professor;
    }

    public Integer getWorkloadHours() {
        return workloadHours;
    }

    public Integer getMaxAbsences() {
        return maxAbsences;
    }

    public String getColor() {
        return color;
    }

    public Status getStatus() {
        return status;
    }
}
