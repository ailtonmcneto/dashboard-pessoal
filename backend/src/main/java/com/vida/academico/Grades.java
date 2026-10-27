package com.vida.academico;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import org.jspecify.annotations.Nullable;

/**
 * Cálculo de média ponderada, numa escala de 0 a 10. Cada nota é normalizada
 * ({@code score / maxScore * 10}) antes de entrar na média.
 */
final class Grades {

    private static final BigDecimal TEN = BigDecimal.TEN;
    private static final int SCALE = 2;

    private Grades() {
    }

    record Item(BigDecimal weight, BigDecimal maxScore, @Nullable BigDecimal score) {
    }

    /**
     * @param average média das avaliações já corrigidas (null se nenhuma)
     * @param gradedWeight soma dos pesos já corrigidos
     * @param pendingWeight soma dos pesos ainda sem nota
     * @param neededOnPending nota média (0–10) necessária no que falta para chegar na média de aprovação;
     *     null quando não falta nada; pode passar de 10 (não dá mais) ou ser ≤ 0 (já garantido)
     */
    record Summary(@Nullable BigDecimal average, BigDecimal gradedWeight, BigDecimal pendingWeight,
            @Nullable BigDecimal neededOnPending) {
    }

    static Summary summarize(Collection<Item> items, BigDecimal passingGrade) {
        BigDecimal points = BigDecimal.ZERO; // soma de nota normalizada × peso
        BigDecimal graded = BigDecimal.ZERO;
        BigDecimal pending = BigDecimal.ZERO;
        for (Item item : items) {
            if (item.score() == null) {
                pending = pending.add(item.weight());
            } else {
                BigDecimal normalized = item.score().multiply(TEN).divide(item.maxScore(), 10, RoundingMode.HALF_UP);
                points = points.add(normalized.multiply(item.weight()));
                graded = graded.add(item.weight());
            }
        }

        BigDecimal average = graded.signum() == 0 ? null : points.divide(graded, SCALE, RoundingMode.HALF_UP);

        BigDecimal needed = null;
        if (pending.signum() > 0) {
            BigDecimal total = graded.add(pending);
            BigDecimal missingPoints = passingGrade.multiply(total).subtract(points);
            needed = missingPoints.divide(pending, SCALE, RoundingMode.HALF_UP);
        }
        return new Summary(average, graded, pending, needed);
    }
}
