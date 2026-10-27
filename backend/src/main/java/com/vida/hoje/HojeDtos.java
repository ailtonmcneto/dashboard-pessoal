package com.vida.hoje;

import com.vida.academico.AcademicDtos.AssessmentResponse;
import com.vida.academico.AcademicDtos.SlotResponse;
import com.vida.academico.AcademicDtos.TopicResponse;
import com.vida.agenda.EventDtos.EventResponse;
import com.vida.metas.GoalDtos.TaskResponse;
import com.vida.saude.HealthDtos.HabitResponse;
import com.vida.saude.HealthDtos.MetricResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class HojeDtos {

    private HojeDtos() {
    }

    public enum Severity { CRITICA, ATENCAO }

    public enum AlertKind {
        CONTA_VENCIDA, FATURA, PROVA_PROXIMA, FALTAS, NOTA_EM_RISCO, TAREFA_ATRASADA, ORCAMENTO, META_ATRASADA
    }

    /** Algo que pede atenção hoje; {@code link} é a rota do front onde resolver. */
    public record Alert(AlertKind kind, Severity severity, String title, String detail, String link) {
    }

    public record Money(BigDecimal totalBalance, BigDecimal income, BigDecimal expense, int pendingCount,
            BigDecimal pendingAmount) {
    }

    /**
     * Tudo que a tela Hoje mostra, numa chamada só. Alertas vêm ordenados (críticos primeiro);
     * hábitos e métricas já filtrados (devidos hoje / com meta).
     */
    public record HojeResponse(
            LocalDate date,
            List<Alert> alerts,
            List<TaskResponse> tasks,
            List<SlotResponse> classes,
            List<AssessmentResponse> assessments,
            List<EventResponse> events,
            int reviewsDue,
            List<TopicResponse> reviews,
            List<HabitResponse> habits,
            List<MetricResponse> metrics,
            Money money,
            int studyMinutesWeek) {
    }
}
