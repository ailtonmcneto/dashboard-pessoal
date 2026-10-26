package com.vida.saude;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Contrato HTTP de saúde e hábitos. Dias da semana no padrão ISO: 1 = segunda … 7 = domingo. */
public final class HealthDtos {

    private HealthDtos() {
    }

    // ---------- Métricas ----------

    public record MetricRequest(
            @NotBlank @Size(max = 40) String name,
            @NotBlank @Size(max = 12) String unit,
            MetricType.@Nullable Aggregation aggregation,
            @Positive @Digits(integer = 8, fraction = 2) @Nullable BigDecimal target,
            MetricType.@Nullable TargetKind targetKind,
            @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "use uma cor no formato #RRGGBB") @Nullable String color,
            @Nullable Boolean archived) {
    }

    public enum DayStatus { SEM_DADOS, SEM_META, ATINGIDA, NAO_ATINGIDA }

    /**
     * {@code today}: valor de hoje pela regra da métrica; {@code average7}: média dos valores diários
     * dos últimos 7 dias que têm registro.
     */
    public record MetricResponse(
            UUID id, String name, String unit, MetricType.Aggregation aggregation, @Nullable BigDecimal target,
            MetricType.@Nullable TargetKind targetKind, String color, boolean archived, @Nullable BigDecimal today,
            DayStatus todayStatus, @Nullable BigDecimal average7, @Nullable BigDecimal lastValue,
            @Nullable Instant lastAt) {
    }

    public record MeasurementRequest(
            @NotNull @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal value,
            @Nullable Instant measuredAt,
            @Size(max = 200) @Nullable String note) {
    }

    public record MeasurementResponse(UUID id, UUID metricTypeId, Instant measuredAt, BigDecimal value,
            @Nullable String note) {
    }

    /** Valor de um dia; null quando não há registro. */
    public record DayValue(LocalDate date, @Nullable BigDecimal value, @Nullable Boolean met) {
    }

    // ---------- Hábitos ----------

    public record HabitRequest(
            @NotBlank @Size(max = 60) String name,
            Habit.@Nullable Schedule schedule,
            @Nullable List<@Min(1) @Max(7) Integer> weekdays,
            @Min(1) @Max(7) @Nullable Integer timesPerWeek,
            @Nullable LocalDate startsOn,
            @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "use uma cor no formato #RRGGBB") @Nullable String color,
            @Nullable Boolean archived) {
    }

    /** Hábitos diários e de dias da semana contam dias seguidos; "X por semana" conta semanas. */
    public enum StreakUnit { DIAS, SEMANAS }

    public record DayCell(LocalDate date, boolean due, boolean done) {
    }

    public record HabitResponse(
            UUID id, String name, Habit.Schedule schedule, List<Integer> weekdays, @Nullable Integer timesPerWeek,
            LocalDate startsOn, String color, boolean archived,
            int currentStreak, int bestStreak, StreakUnit streakUnit,
            boolean dueToday, boolean doneToday, int doneThisWeek, int targetThisWeek, int rate30,
            List<DayCell> lastDays) {
    }
}
