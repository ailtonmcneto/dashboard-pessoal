package com.vida.academico;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Contrato HTTP do módulo acadêmico. Sem {@code @Nullable} = obrigatório. */
public final class AcademicDtos {

    private AcademicDtos() {
    }

    // ---------- Semestres ----------

    public record TermRequest(
            @NotBlank @Size(max = 60) String name,
            @NotNull LocalDate startsOn,
            @NotNull LocalDate endsOn,
            @DecimalMin("0") @DecimalMax("10") @Nullable BigDecimal passingGrade) {
    }

    public record TermResponse(UUID id, String name, LocalDate startsOn, LocalDate endsOn, BigDecimal passingGrade,
            boolean current) {
    }

    // ---------- Disciplinas ----------

    public record CourseRequest(
            @NotNull UUID termId,
            @NotBlank @Size(max = 120) String name,
            @Size(max = 30) @Nullable String code,
            @Size(max = 120) @Nullable String professor,
            @Positive @Nullable Integer workloadHours,
            @PositiveOrZero @Nullable Integer maxAbsences,
            @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "use uma cor no formato #RRGGBB") @Nullable String color,
            Course.@Nullable Status status) {
    }

    /** Situação da nota, comparando a média com a nota de aprovação do semestre. */
    public enum GradeStatus {
        /** nenhuma avaliação corrigida ainda */
        SEM_NOTAS,
        /** a nota necessária no que falta é até a média de aprovação */
        EM_DIA,
        /** precisa de mais que a média no que falta, mas ainda dá */
        EM_RISCO,
        /** já aprovado pela nota (mesmo zerando o que falta) */
        APROVADO,
        /** sem chance pela nota */
        REPROVADO
    }

    public enum AbsenceStatus { SEM_LIMITE, OK, ATENCAO, ESTOURADO }

    public record GradeSummary(
            @Nullable BigDecimal average,
            BigDecimal passingGrade,
            BigDecimal gradedWeight,
            BigDecimal pendingWeight,
            @Nullable BigDecimal neededOnPending,
            GradeStatus status) {
    }

    public record AbsenceSummary(int used, @Nullable Integer max, AbsenceStatus status) {
    }

    public record CourseResponse(
            UUID id, UUID termId, String name, @Nullable String code, @Nullable String professor,
            @Nullable Integer workloadHours, @Nullable Integer maxAbsences, String color, Course.Status status,
            GradeSummary grades, AbsenceSummary absences) {
    }

    // ---------- Horário ----------

    public record SlotRequest(
            @NotNull @Min(1) @Max(7) Integer weekday,
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime,
            @Size(max = 60) @Nullable String room) {
    }

    /** weekday ISO: 1 = segunda … 7 = domingo. */
    public record SlotResponse(UUID id, UUID courseId, String courseName, String color, int weekday,
            LocalTime startTime, LocalTime endTime, @Nullable String room) {
    }

    // ---------- Faltas ----------

    public record AbsenceRequest(
            @NotNull LocalDate date,
            @Min(1) @Max(20) @Nullable Integer classesMissed,
            @Size(max = 200) @Nullable String note) {
    }

    public record AbsenceResponse(UUID id, UUID courseId, LocalDate date, int classesMissed, @Nullable String note) {
    }

    // ---------- Avaliações ----------

    public record AssessmentRequest(
            @NotBlank @Size(max = 120) String title,
            Assessment.@Nullable Kind kind,
            @Nullable LocalDate dueOn,
            @Positive @Nullable BigDecimal weight,
            @Positive @Nullable BigDecimal maxScore,
            @PositiveOrZero @Nullable BigDecimal score,
            @Nullable Set<UUID> topicIds) {
    }

    public record AssessmentResponse(
            UUID id, UUID courseId, String courseName, String color, String title, Assessment.Kind kind,
            @Nullable LocalDate dueOn, BigDecimal weight, BigDecimal maxScore, @Nullable BigDecimal score,
            @Nullable BigDecimal normalizedScore, Set<UUID> topicIds) {
    }

    // ---------- Cursos livres, livros… ----------

    public record ResourceRequest(
            @NotBlank @Size(max = 200) String title,
            Resource.@Nullable Kind kind,
            @Size(max = 120) @Nullable String provider,
            @Size(max = 500) @Pattern(regexp = "^https?://.*", message = "use um link http(s)") @Nullable String url,
            @Positive @Nullable Integer totalUnits,
            @PositiveOrZero @Nullable Integer doneUnits,
            Resource.@Nullable Status status,
            @Nullable LocalDate startedOn,
            @Nullable LocalDate finishedOn) {
    }

    public record ResourceResponse(
            UUID id, String title, Resource.Kind kind, @Nullable String provider, @Nullable String url,
            @Nullable Integer totalUnits, int doneUnits, @Nullable Integer progressPercent, Resource.Status status,
            @Nullable LocalDate startedOn, @Nullable LocalDate finishedOn) {
    }

    // ---------- Assuntos ----------

    public record TopicRequest(
            @NotBlank @Size(max = 200) String title,
            @Nullable UUID courseId,
            @Nullable UUID resourceId,
            @Nullable UUID parentId) {
    }

    public record ReviewRequest(@NotNull @Min(0) @Max(5) Integer mastery) {
    }

    public record TopicResponse(
            UUID id, String title, @Nullable UUID courseId, @Nullable UUID resourceId, @Nullable UUID parentId,
            int mastery, @Nullable LocalDate lastReviewedOn, @Nullable LocalDate nextReviewOn, boolean due) {
    }

    // ---------- Sessões de estudo ----------

    public record StudySessionRequest(
            @NotNull Instant startedAt,
            @NotNull @Min(1) @Max(1440) Integer minutes,
            @Nullable UUID courseId,
            @Nullable UUID resourceId,
            @Nullable UUID topicId,
            @Size(max = 1000) @Nullable String notes) {
    }

    public record StudySessionResponse(
            UUID id, Instant startedAt, int minutes, @Nullable UUID courseId, @Nullable UUID resourceId,
            @Nullable UUID topicId, @Nullable String notes, String label) {
    }

    /** Minutos por disciplina/curso (ou "Sem vínculo") e por dia, no fuso do usuário. */
    public record StudySummary(int totalMinutes, List<StudyBucket> bySubject, List<StudyDay> byDay) {
    }

    public record StudyBucket(@Nullable UUID courseId, @Nullable UUID resourceId, String label,
            @Nullable String color, int minutes) {
    }

    public record StudyDay(LocalDate date, int minutes) {
    }
}
