package com.vida.academico;

import com.vida.common.OwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

/** Curso livre, livro, vídeo… — estudo fora da faculdade. */
@Entity
@Table(name = "resources")
public class Resource extends OwnedEntity {

    public enum Kind { CURSO, LIVRO, VIDEO, ARTIGO, OUTRO }

    public enum Status { PLANEJADO, ATIVO, PAUSADO, CONCLUIDO }

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Kind kind;

    private String provider;

    private String url;

    /** Aulas, capítulos, páginas… o que fizer sentido para o recurso. */
    private Integer totalUnits;

    @Column(nullable = false)
    private int doneUnits;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    private LocalDate startedOn;

    private LocalDate finishedOn;

    protected Resource() {
    }

    Resource(UUID userId) {
        super(userId);
    }

    void update(String title, Kind kind, String provider, String url, Integer totalUnits, int doneUnits,
            Status status, LocalDate startedOn, LocalDate finishedOn) {
        this.title = title;
        this.kind = kind;
        this.provider = provider;
        this.url = url;
        this.totalUnits = totalUnits;
        this.doneUnits = doneUnits;
        this.status = status;
        this.startedOn = startedOn;
        this.finishedOn = finishedOn;
    }

    public String getTitle() {
        return title;
    }

    public Kind getKind() {
        return kind;
    }

    public String getProvider() {
        return provider;
    }

    public String getUrl() {
        return url;
    }

    public Integer getTotalUnits() {
        return totalUnits;
    }

    public int getDoneUnits() {
        return doneUnits;
    }

    public Status getStatus() {
        return status;
    }

    public LocalDate getStartedOn() {
        return startedOn;
    }

    public LocalDate getFinishedOn() {
        return finishedOn;
    }
}
