package com.vida.agenda;

import com.vida.common.OwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "events")
public class Event extends OwnedEntity {

    public enum Kind { COMPROMISSO, CONSULTA, OUTRO }

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Kind kind;

    @Column(nullable = false)
    private Instant startsAt;

    private Instant endsAt;

    private String location;

    private String notes;

    protected Event() {
    }

    Event(UUID userId) {
        super(userId);
    }

    void update(String title, Kind kind, Instant startsAt, Instant endsAt, String location, String notes) {
        this.title = title;
        this.kind = kind;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.location = location;
        this.notes = notes;
    }

    public String getTitle() {
        return title;
    }

    public Kind getKind() {
        return kind;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public String getLocation() {
        return location;
    }

    public String getNotes() {
        return notes;
    }
}
