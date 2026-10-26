package com.vida.saude;

import com.vida.common.OwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "measurements")
public class Measurement extends OwnedEntity {

    @Column(nullable = false, updatable = false)
    private UUID metricTypeId;

    @Column(nullable = false)
    private Instant measuredAt;

    @Column(nullable = false)
    private BigDecimal value;

    private String note;

    protected Measurement() {
    }

    Measurement(UUID userId, UUID metricTypeId, Instant measuredAt, BigDecimal value, String note) {
        super(userId);
        this.metricTypeId = metricTypeId;
        this.measuredAt = measuredAt;
        this.value = value;
        this.note = note;
    }

    public UUID getMetricTypeId() {
        return metricTypeId;
    }

    public Instant getMeasuredAt() {
        return measuredAt;
    }

    public BigDecimal getValue() {
        return value;
    }

    public String getNote() {
        return note;
    }
}
