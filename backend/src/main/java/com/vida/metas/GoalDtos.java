package com.vida.metas;

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

/** Contrato HTTP de metas, marcos, projetos e tarefas. */
public final class GoalDtos {

    private GoalDtos() {
    }

    // ---------- Metas ----------

    public record GoalRequest(
            @NotBlank @Size(max = 120) String title,
            @Size(max = 2000) @Nullable String description,
            Goal.@Nullable Area area,
            @Nullable LocalDate startsOn,
            @Nullable LocalDate dueOn,
            Goal.@Nullable ProgressMode progressMode,
            @Positive @Digits(integer = 12, fraction = 2) @Nullable BigDecimal targetValue,
            @PositiveOrZero @Digits(integer = 12, fraction = 2) @Nullable BigDecimal currentValue,
            @Size(max = 20) @Nullable String unit,
            @Nullable UUID accountId) {
    }

    public record StatusRequest(@NotNull Goal.Status status) {
    }

    /**
     * Ritmo: compara o progresso com o esperado pelo tempo decorrido entre início e prazo
     * (margem de 10 pontos para "no ritmo").
     */
    public enum Pace { SEM_PRAZO, ADIANTADA, NO_RITMO, ATRASADA, CONCLUIDA, ABANDONADA }

    public record MilestoneResponse(UUID id, String title, @Nullable LocalDate dueOn, @Nullable LocalDate doneOn,
            boolean done, int position) {
    }

    /**
     * {@code current}/{@code target} seguem o modo: valor manual, marcos ou tarefas concluídos, ou saldo
     * da conta. {@code percent} vai de 0 a 100; {@code expectedPercent} é o esperado pela data.
     */
    public record GoalResponse(
            UUID id, String title, @Nullable String description, Goal.Area area, Goal.Status status,
            @Nullable LocalDate startsOn, @Nullable LocalDate dueOn, @Nullable LocalDate completedOn,
            Goal.ProgressMode progressMode, @Nullable BigDecimal targetValue, @Nullable BigDecimal currentValue,
            @Nullable String unit, @Nullable UUID accountId, @Nullable String accountName,
            BigDecimal current, @Nullable BigDecimal target, int percent, @Nullable Integer expectedPercent,
            Pace pace, List<MilestoneResponse> milestones, int openTasks, int doneTasks) {
    }

    public record MilestoneRequest(
            @NotBlank @Size(max = 120) String title,
            @Nullable LocalDate dueOn) {
    }

    // ---------- Projetos ----------

    public record ProjectRequest(
            @NotBlank @Size(max = 120) String title,
            @Size(max = 2000) @Nullable String description,
            Project.@Nullable Kind kind,
            Project.@Nullable Status status,
            @Nullable LocalDate startsOn,
            @Nullable LocalDate dueOn,
            @Size(max = 500) @Pattern(regexp = "^https?://.*", message = "use um link http(s)") @Nullable String url,
            @Nullable UUID goalId,
            @Nullable UUID courseId) {
    }

    public record ProjectResponse(
            UUID id, String title, @Nullable String description, Project.Kind kind, Project.Status status,
            @Nullable LocalDate startsOn, @Nullable LocalDate dueOn, @Nullable String url,
            @Nullable UUID goalId, @Nullable String goalTitle, @Nullable UUID courseId, @Nullable String courseName,
            int openTasks, int doneTasks, int percent, boolean overdue) {
    }

    // ---------- Tarefas ----------

    /** HOJE: vencidas e de hoje; SEMANA: até 6 dias à frente (e vencidas); ABERTAS; FEITAS (últimos 30 dias). */
    public enum TaskView { HOJE, SEMANA, ABERTAS, FEITAS }

    public record TaskRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) @Nullable String notes,
            @Min(1) @Max(3) @Nullable Integer priority,
            @Nullable LocalDate dueOn,
            @Nullable UUID goalId,
            @Nullable UUID milestoneId,
            @Nullable UUID projectId,
            @Nullable UUID courseId) {
    }

    public record TaskResponse(
            UUID id, String title, @Nullable String notes, int priority, @Nullable LocalDate dueOn, boolean done,
            @Nullable Instant doneAt, boolean overdue,
            @Nullable UUID goalId, @Nullable String goalTitle, @Nullable UUID milestoneId,
            @Nullable UUID projectId, @Nullable String projectTitle,
            @Nullable UUID courseId, @Nullable String courseName) {
    }
}
