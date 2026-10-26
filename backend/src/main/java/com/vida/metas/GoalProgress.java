package com.vida.metas;

import com.vida.metas.GoalDtos.Pace;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.jspecify.annotations.Nullable;

/** Regras de progresso e ritmo de uma meta. Sem estado e sem banco: fácil de testar. */
final class GoalProgress {

    /** Tolerância, em pontos percentuais, para considerar "no ritmo". */
    static final int PACE_MARGIN = 10;

    private GoalProgress() {
    }

    /** current ÷ target em 0–100 (negativo vira 0, acima do alvo vira 100). */
    static int percent(BigDecimal current, @Nullable BigDecimal target) {
        if (target == null || target.signum() <= 0 || current.signum() <= 0) {
            return 0;
        }
        int p = current.multiply(BigDecimal.valueOf(100)).divide(target, 0, RoundingMode.DOWN).intValue();
        return Math.min(100, p);
    }

    /** Quanto já deveria estar feito, pelo tempo decorrido entre início e prazo; null sem prazo. */
    static @Nullable Integer expected(@Nullable LocalDate startsOn, @Nullable LocalDate dueOn, LocalDate today) {
        if (startsOn == null || dueOn == null) {
            return null;
        }
        long total = ChronoUnit.DAYS.between(startsOn, dueOn);
        if (total <= 0) {
            return today.isBefore(dueOn) ? 0 : 100;
        }
        long elapsed = ChronoUnit.DAYS.between(startsOn, today);
        return (int) Math.max(0, Math.min(100, elapsed * 100 / total));
    }

    static Pace pace(Goal.Status status, int percent, @Nullable Integer expected, @Nullable LocalDate dueOn,
            LocalDate today) {
        if (status == Goal.Status.CONCLUIDA) {
            return Pace.CONCLUIDA;
        }
        if (status == Goal.Status.ABANDONADA) {
            return Pace.ABANDONADA;
        }
        if (percent >= 100) {
            return Pace.ADIANTADA;
        }
        if (dueOn == null || expected == null) {
            return Pace.SEM_PRAZO;
        }
        if (today.isAfter(dueOn)) {
            return Pace.ATRASADA;
        }
        if (percent >= expected + PACE_MARGIN) {
            return Pace.ADIANTADA;
        }
        return percent >= expected - PACE_MARGIN ? Pace.NO_RITMO : Pace.ATRASADA;
    }
}
