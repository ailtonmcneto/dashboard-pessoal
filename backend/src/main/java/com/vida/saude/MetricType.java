package com.vida.saude;

import com.vida.common.OwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

/** O que se mede (sono, peso, água…), com unidade, regra do valor diário e meta opcional. */
@Entity
@Table(name = "metric_types")
public class MetricType extends OwnedEntity {

    /** Como os registros de um mesmo dia viram um valor. */
    public enum Aggregation { SOMA, MEDIA, ULTIMO }

    /** Meta diária: chegar pelo menos a (MINIMO) ou não passar de (MAXIMO). */
    public enum TargetKind { MINIMO, MAXIMO }

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Aggregation aggregation;

    private BigDecimal target;

    @Enumerated(EnumType.STRING)
    private TargetKind targetKind;

    @Column(nullable = false)
    private String color;

    @Column(nullable = false)
    private boolean archived;

    protected MetricType() {
    }

    MetricType(UUID userId) {
        super(userId);
    }

    void update(String name, String unit, Aggregation aggregation, BigDecimal target, TargetKind targetKind,
            String color, boolean archived) {
        this.name = name;
        this.unit = unit;
        this.aggregation = aggregation;
        this.target = target;
        this.targetKind = targetKind;
        this.color = color;
        this.archived = archived;
    }

    public String getName() {
        return name;
    }

    public String getUnit() {
        return unit;
    }

    public Aggregation getAggregation() {
        return aggregation;
    }

    public BigDecimal getTarget() {
        return target;
    }

    public TargetKind getTargetKind() {
        return targetKind;
    }

    public String getColor() {
        return color;
    }

    public boolean isArchived() {
        return archived;
    }
}
