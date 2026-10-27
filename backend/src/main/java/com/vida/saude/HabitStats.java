package com.vida.saude;

import com.vida.saude.HealthDtos.StreakUnit;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/**
 * Estatísticas de um hábito a partir dos dias cumpridos. Sem estado e sem banco: fácil de testar.
 *
 * <p>Hoje fica "em aberto": se ainda não foi cumprido, não quebra a sequência. Nos hábitos
 * diários e de dias da semana a sequência conta dias devidos seguidos (dias não devidos são
 * pulados); no "X vezes por semana" conta semanas (seg–dom) que bateram a meta.
 */
final class HabitStats {

    /** Limite de quanto voltar no tempo ao calcular sequências. */
    static final int HISTORY_DAYS = 400;

    record Rule(Habit.Schedule schedule, int weekdaysMask, int timesPerWeek, LocalDate startsOn) {
    }

    /**
     * @param rate30 percentual cumprido nos últimos 30 dias (no semanal, sobre 4 semanas)
     */
    record Stats(int currentStreak, int bestStreak, StreakUnit streakUnit, boolean dueToday, boolean doneToday,
            int doneThisWeek, int targetThisWeek, int rate30) {
    }

    private HabitStats() {
    }

    static short mask(Collection<DayOfWeek> days) {
        int mask = 0;
        for (DayOfWeek d : days) {
            mask |= 1 << (d.getValue() - 1);
        }
        return (short) mask;
    }

    static Set<DayOfWeek> weekdays(int mask) {
        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        for (DayOfWeek d : DayOfWeek.values()) {
            if ((mask & (1 << (d.getValue() - 1))) != 0) {
                days.add(d);
            }
        }
        return days;
    }

    /** Se o hábito "pede" esse dia (no semanal, qualquer dia serve). */
    static boolean isDue(Rule rule, LocalDate day) {
        if (day.isBefore(rule.startsOn())) {
            return false;
        }
        return switch (rule.schedule()) {
            case DIARIO, X_POR_SEMANA -> true;
            case DIAS_SEMANA -> (rule.weekdaysMask() & (1 << (day.getDayOfWeek().getValue() - 1))) != 0;
        };
    }

    static Stats compute(Rule rule, Set<LocalDate> done, LocalDate today) {
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        int doneThisWeek = (int) done.stream().filter(d -> !d.isBefore(monday) && !d.isAfter(today)).count();
        boolean doneToday = done.contains(today);

        if (rule.schedule() == Habit.Schedule.X_POR_SEMANA) {
            int target = rule.timesPerWeek();
            boolean dueToday = !today.isBefore(rule.startsOn()) && (doneThisWeek < target || doneToday);
            int recent = (int) done.stream().filter(d -> !d.isBefore(today.minusDays(27)) && !d.isAfter(today)).count();
            int rate = Math.min(100, recent * 100 / (target * 4));
            return new Stats(weeklyStreak(rule, done, monday, doneThisWeek >= target), bestWeeklyStreak(rule, done, monday),
                    StreakUnit.SEMANAS, dueToday, doneToday, doneThisWeek, target, rate);
        }

        int targetThisWeek = 0;
        for (LocalDate d = monday; !d.isAfter(monday.plusDays(6)); d = d.plusDays(1)) {
            if (isDue(rule, d)) {
                targetThisWeek++;
            }
        }
        return new Stats(dailyStreak(rule, done, today), bestDailyStreak(rule, done, today), StreakUnit.DIAS,
                isDue(rule, today), doneToday, doneThisWeek, targetThisWeek, dailyRate(rule, done, today));
    }

    private static int dailyStreak(Rule rule, Set<LocalDate> done, LocalDate today) {
        int streak = 0;
        LocalDate limit = maxDate(rule.startsOn(), today.minusDays(HISTORY_DAYS));
        for (LocalDate d = today; !d.isBefore(limit); d = d.minusDays(1)) {
            if (!isDue(rule, d)) {
                continue;
            }
            if (done.contains(d)) {
                streak++;
            } else if (!d.equals(today)) {
                break;
            }
        }
        return streak;
    }

    private static int bestDailyStreak(Rule rule, Set<LocalDate> done, LocalDate today) {
        int best = 0;
        int run = 0;
        for (LocalDate d = maxDate(rule.startsOn(), today.minusDays(HISTORY_DAYS)); !d.isAfter(today); d = d.plusDays(1)) {
            if (!isDue(rule, d)) {
                continue;
            }
            if (done.contains(d)) {
                best = Math.max(best, ++run);
            } else if (!d.equals(today)) {
                run = 0;
            }
        }
        return best;
    }

    /** Dias devidos cumpridos ÷ dias devidos nos últimos 30 (hoje só conta se já foi cumprido). */
    private static int dailyRate(Rule rule, Set<LocalDate> done, LocalDate today) {
        int due = 0;
        int hit = 0;
        for (LocalDate d = today.minusDays(29); !d.isAfter(today); d = d.plusDays(1)) {
            if (!isDue(rule, d) || (d.equals(today) && !done.contains(d))) {
                continue;
            }
            due++;
            if (done.contains(d)) {
                hit++;
            }
        }
        return due == 0 ? 0 : hit * 100 / due;
    }

    private static int weeklyStreak(Rule rule, Set<LocalDate> done, LocalDate thisMonday, boolean thisWeekMet) {
        int streak = thisWeekMet ? 1 : 0;
        LocalDate firstMonday = rule.startsOn().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        for (LocalDate monday = thisMonday.minusWeeks(1); !monday.isBefore(firstMonday)
                && !monday.isBefore(thisMonday.minusDays(HISTORY_DAYS)); monday = monday.minusWeeks(1)) {
            if (countInWeek(done, monday) >= rule.timesPerWeek()) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }

    private static int bestWeeklyStreak(Rule rule, Set<LocalDate> done, LocalDate thisMonday) {
        LocalDate firstMonday = maxDate(rule.startsOn(), thisMonday.minusDays(HISTORY_DAYS))
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        int best = 0;
        int run = 0;
        for (LocalDate monday = firstMonday; !monday.isAfter(thisMonday); monday = monday.plusWeeks(1)) {
            if (countInWeek(done, monday) >= rule.timesPerWeek()) {
                best = Math.max(best, ++run);
            } else if (!monday.equals(thisMonday)) {
                run = 0;
            }
        }
        return best;
    }

    private static int countInWeek(Set<LocalDate> done, LocalDate monday) {
        LocalDate sunday = monday.plusDays(6);
        return (int) done.stream().filter(d -> !d.isBefore(monday) && !d.isAfter(sunday)).count();
    }

    private static LocalDate maxDate(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }
}
