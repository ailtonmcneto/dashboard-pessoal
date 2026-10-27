package com.vida.academico;

import com.vida.common.OwnedEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Prova, trabalho, seminário… com peso e nota (null até sair). */
@Entity
@Table(name = "assessments")
public class Assessment extends OwnedEntity {

    public enum Kind { PROVA, TRABALHO, SEMINARIO, LISTA, PROJETO, OUTRO }

    /** Linha de assessment_topics; leva o user_id para a FK composta com topics. */
    @Embeddable
    record TopicLink(@Column(name = "user_id", nullable = false) UUID userId,
            @Column(name = "topic_id", nullable = false) UUID topicId) {
    }

    @Column(nullable = false, updatable = false)
    private UUID courseId;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Kind kind;

    private LocalDate dueOn;

    @Column(nullable = false)
    private BigDecimal weight;

    @Column(nullable = false)
    private BigDecimal maxScore;

    private BigDecimal score;

    @ElementCollection
    @CollectionTable(name = "assessment_topics", joinColumns = @JoinColumn(name = "assessment_id"))
    private Set<TopicLink> topics = new HashSet<>();

    protected Assessment() {
    }

    Assessment(UUID userId, UUID courseId) {
        super(userId);
        this.courseId = courseId;
    }

    void update(String title, Kind kind, LocalDate dueOn, BigDecimal weight, BigDecimal maxScore, BigDecimal score,
            Set<UUID> topicIds) {
        this.title = title;
        this.kind = kind;
        this.dueOn = dueOn;
        this.weight = weight;
        this.maxScore = maxScore;
        this.score = score;
        this.topics.clear();
        topicIds.forEach(id -> this.topics.add(new TopicLink(getUserId(), id)));
    }

    public UUID getCourseId() {
        return courseId;
    }

    public String getTitle() {
        return title;
    }

    public Kind getKind() {
        return kind;
    }

    public LocalDate getDueOn() {
        return dueOn;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public BigDecimal getScore() {
        return score;
    }

    public Set<UUID> getTopicIds() {
        return topics.stream().map(TopicLink::topicId).collect(Collectors.toSet());
    }
}
