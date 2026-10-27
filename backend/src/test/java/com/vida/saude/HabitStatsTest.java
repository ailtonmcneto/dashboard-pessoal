package com.vida.saude;

import static org.assertj.core.api.Assertions.assertThat;

import com.vida.saude.HealthDtos.StreakUnit;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HabitStatsTest {

    // quinta-feira
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 22);

    private static Set<LocalDate> days(int... daysAgo) {
        Set<LocalDate> set = new HashSet<>();
        for (int d : daysAgo) {
            set.add(TODAY.minusDays(d));
        }
        return set;
    }

    private static HabitStats.Rule daily() {
        return new HabitStats.Rule(Habit.Schedule.DIARIO, 0, 0, TODAY.minusDays(60));
    }

    @Test
    void diarioHojeEmAbertoNaoQuebraASequencia() {
        HabitStats.Stats s = HabitStats.compute(daily(), days(1, 2, 3), TODAY);
        assertThat(s.currentStreak()).isEqualTo(3);
        assertThat(s.doneToday()).isFalse();
        assertThat(s.dueToday()).isTrue();
        assertThat(s.streakUnit()).isEqualTo(StreakUnit.DIAS);

        assertThat(HabitStats.compute(daily(), days(0, 1, 2, 3), TODAY).currentStreak()).isEqualTo(4);
        // ontem falhou: sequência zera mesmo com dias antes
        assertThat(HabitStats.compute(daily(), days(2, 3, 4), TODAY).currentStreak()).isZero();
    }

    @Test
    void melhorSequenciaGuardaORecorde() {
        HabitStats.Stats s = HabitStats.compute(daily(), days(1, 10, 11, 12, 13, 14), TODAY);
        assertThat(s.currentStreak()).isEqualTo(1);
        assertThat(s.bestStreak()).isEqualTo(5);
    }

    @Test
    void diasDaSemanaPulamOsDiasQueNaoContam() {
        // seg, qua, sex
        short mask = HabitStats.mask(List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY));
        HabitStats.Rule rule = new HabitStats.Rule(Habit.Schedule.DIAS_SEMANA, mask, 0, TODAY.minusDays(60));
        // qua (1 dia atrás), seg (3), sex anterior (6), qua anterior (8)
        HabitStats.Stats s = HabitStats.compute(rule, days(1, 3, 6, 8), TODAY);
        assertThat(s.currentStreak()).isEqualTo(4);
        assertThat(s.dueToday()).isFalse(); // quinta
        assertThat(s.targetThisWeek()).isEqualTo(3);
        assertThat(s.doneThisWeek()).isEqualTo(2);
        assertThat(HabitStats.weekdays(mask)).containsExactlyInAnyOrder(
                DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
    }

    @Test
    void xPorSemanaContaSemanas() {
        HabitStats.Rule rule = new HabitStats.Rule(Habit.Schedule.X_POR_SEMANA, 0, 3, TODAY.minusDays(60));
        // semana atual (seg 19 → qui 22): 2 vezes; anteriores: 3 e 3; a de antes: 1
        Set<LocalDate> done = days(1, 3, 7, 8, 9, 14, 15, 16, 21);
        HabitStats.Stats s = HabitStats.compute(rule, done, TODAY);
        assertThat(s.streakUnit()).isEqualTo(StreakUnit.SEMANAS);
        assertThat(s.doneThisWeek()).isEqualTo(2);
        assertThat(s.targetThisWeek()).isEqualTo(3);
        assertThat(s.currentStreak()).isEqualTo(2); // semana atual ainda em aberto
        assertThat(s.dueToday()).isTrue();

        done.add(TODAY);
        HabitStats.Stats met = HabitStats.compute(rule, done, TODAY);
        assertThat(met.currentStreak()).isEqualTo(3);
        assertThat(met.dueToday()).isTrue(); // já marcado hoje continua aparecendo
    }

    @Test
    void antesDoInicioNaoConta() {
        HabitStats.Rule rule = new HabitStats.Rule(Habit.Schedule.DIARIO, 0, 0, TODAY.minusDays(2));
        HabitStats.Stats s = HabitStats.compute(rule, days(1, 2), TODAY);
        assertThat(s.currentStreak()).isEqualTo(2);
        assertThat(s.rate30()).isEqualTo(100); // só 2 dias devidos, os 2 cumpridos
        assertThat(HabitStats.isDue(rule, TODAY.minusDays(3))).isFalse();
    }
}
