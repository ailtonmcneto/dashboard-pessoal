package com.vida.metas;

import static org.assertj.core.api.Assertions.assertThat;

import com.vida.metas.GoalDtos.Pace;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class GoalProgressTest {

    private static final LocalDate START = LocalDate.of(2026, 1, 1);
    private static final LocalDate DUE = LocalDate.of(2026, 1, 11); // 10 dias

    @Test
    void percentualLimitadoEntreZeroECem() {
        assertThat(GoalProgress.percent(new BigDecimal("3"), new BigDecimal("12"))).isEqualTo(25);
        assertThat(GoalProgress.percent(new BigDecimal("15"), new BigDecimal("12"))).isEqualTo(100);
        assertThat(GoalProgress.percent(new BigDecimal("-50"), new BigDecimal("100"))).isZero();
        assertThat(GoalProgress.percent(BigDecimal.ZERO, BigDecimal.ZERO)).isZero(); // meta sem marcos
    }

    @Test
    void esperadoPeloTempoDecorrido() {
        assertThat(GoalProgress.expected(START, DUE, START)).isZero();
        assertThat(GoalProgress.expected(START, DUE, START.plusDays(5))).isEqualTo(50);
        assertThat(GoalProgress.expected(START, DUE, DUE.plusDays(3))).isEqualTo(100);
        assertThat(GoalProgress.expected(START, null, START)).isNull();
    }

    @Test
    void ritmoComMargemDeDezPontos() {
        LocalDate mid = START.plusDays(5); // esperado 50%
        assertThat(GoalProgress.pace(Goal.Status.ATIVA, 45, 50, DUE, mid)).isEqualTo(Pace.NO_RITMO);
        assertThat(GoalProgress.pace(Goal.Status.ATIVA, 39, 50, DUE, mid)).isEqualTo(Pace.ATRASADA);
        assertThat(GoalProgress.pace(Goal.Status.ATIVA, 60, 50, DUE, mid)).isEqualTo(Pace.ADIANTADA);
        assertThat(GoalProgress.pace(Goal.Status.ATIVA, 90, 100, DUE, DUE.plusDays(1))).isEqualTo(Pace.ATRASADA);
        assertThat(GoalProgress.pace(Goal.Status.ATIVA, 100, 50, DUE, mid)).isEqualTo(Pace.ADIANTADA);
        assertThat(GoalProgress.pace(Goal.Status.ATIVA, 10, null, null, mid)).isEqualTo(Pace.SEM_PRAZO);
        assertThat(GoalProgress.pace(Goal.Status.CONCLUIDA, 10, 50, DUE, mid)).isEqualTo(Pace.CONCLUIDA);
        assertThat(GoalProgress.pace(Goal.Status.ABANDONADA, 10, 50, DUE, mid)).isEqualTo(Pace.ABANDONADA);
    }
}
