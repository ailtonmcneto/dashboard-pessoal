package com.vida.hoje;

import com.vida.academico.AcademicDtos.AbsenceStatus;
import com.vida.academico.AcademicDtos.AssessmentResponse;
import com.vida.academico.AcademicDtos.CourseResponse;
import com.vida.academico.AcademicDtos.GradeStatus;
import com.vida.academico.AcademicDtos.SlotResponse;
import com.vida.academico.AcademicDtos.TopicResponse;
import com.vida.academico.AssessmentService;
import com.vida.academico.Course;
import com.vida.academico.CourseService;
import com.vida.academico.StudySessionService;
import com.vida.academico.TermService;
import com.vida.academico.TopicService;
import com.vida.agenda.EventDtos.EventResponse;
import com.vida.agenda.EventService;
import com.vida.common.CurrentUser;
import com.vida.financas.BudgetService;
import com.vida.financas.CardService;
import com.vida.financas.CategoryService;
import com.vida.financas.FinanceDtos.BudgetStatus;
import com.vida.financas.FinanceDtos.CardResponse;
import com.vida.financas.FinanceDtos.CategoryResponse;
import com.vida.financas.FinanceDtos.InvoiceStatus;
import com.vida.financas.FinanceDtos.InvoiceSummary;
import com.vida.financas.FinanceDtos.MonthSummary;
import com.vida.financas.FinanceDtos.OverdueBills;
import com.vida.financas.ReportService;
import com.vida.hoje.HojeDtos.Alert;
import com.vida.hoje.HojeDtos.AlertKind;
import com.vida.hoje.HojeDtos.HojeResponse;
import com.vida.hoje.HojeDtos.Money;
import com.vida.hoje.HojeDtos.Severity;
import com.vida.metas.Goal;
import com.vida.metas.GoalDtos.GoalResponse;
import com.vida.metas.GoalDtos.Pace;
import com.vida.metas.GoalDtos.TaskResponse;
import com.vida.metas.GoalDtos.TaskView;
import com.vida.metas.GoalService;
import com.vida.metas.TaskService;
import com.vida.saude.HabitService;
import com.vida.saude.HealthDtos.HabitResponse;
import com.vida.saude.HealthDtos.MetricResponse;
import com.vida.saude.MetricService;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Monta a tela Hoje numa chamada só, reaproveitando os services públicos de cada módulo (que já
 * filtram pelo usuário logado), e calcula os alertas do dia.
 */
@Service
public class HojeService {

    static final int ASSESSMENT_DAYS = 14;
    static final int EVENT_DAYS = 7;
    static final int REVIEW_PREVIEW = 5;
    /** Fatura fechada vira alerta quando vence em até tantos dias. */
    static final int INVOICE_WARNING_DAYS = 5;

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd/MM");

    private final CurrentUser currentUser;
    private final TaskService tasks;
    private final GoalService goals;
    private final TermService terms;
    private final CourseService courses;
    private final AssessmentService assessments;
    private final TopicService topics;
    private final StudySessionService studySessions;
    private final EventService events;
    private final HabitService habits;
    private final MetricService metrics;
    private final ReportService reports;
    private final BudgetService budgets;
    private final CardService cards;
    private final CategoryService categories;

    HojeService(CurrentUser currentUser, TaskService tasks, GoalService goals, TermService terms,
            CourseService courses, AssessmentService assessments, TopicService topics,
            StudySessionService studySessions, EventService events, HabitService habits, MetricService metrics,
            ReportService reports, BudgetService budgets, CardService cards, CategoryService categories) {
        this.currentUser = currentUser;
        this.tasks = tasks;
        this.goals = goals;
        this.terms = terms;
        this.courses = courses;
        this.assessments = assessments;
        this.topics = topics;
        this.studySessions = studySessions;
        this.events = events;
        this.habits = habits;
        this.metrics = metrics;
        this.reports = reports;
        this.budgets = budgets;
        this.cards = cards;
        this.categories = categories;
    }

    @Transactional(readOnly = true)
    public HojeResponse today() {
        LocalDate today = currentUser.today();
        ZoneId zone = currentUser.zone();
        int weekday = today.getDayOfWeek().getValue();

        List<TaskResponse> todayTasks = tasks.list(TaskView.HOJE, null, null, null);
        List<SlotResponse> classes = courses.schedule(null).stream().filter(s -> s.weekday() == weekday).toList();
        List<AssessmentResponse> upcoming = assessments.upcoming(ASSESSMENT_DAYS);
        List<EventResponse> nextEvents = events.list(today.atStartOfDay(zone).toInstant(),
                        today.plusDays(EVENT_DAYS).atStartOfDay(zone).toInstant())
                .stream().map(EventResponse::of).toList();
        List<TopicResponse> due = topics.list(null, null, true);
        List<HabitResponse> habitsToday = habits.list().stream().filter(h -> !h.archived() && h.dueToday()).toList();
        List<MetricResponse> metricsWithTarget = metrics.list().stream()
                .filter(m -> !m.archived() && m.target() != null).toList();
        MonthSummary month = reports.summary(YearMonth.from(today));
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        int studyMinutes = studySessions.summary(monday.atStartOfDay(zone).toInstant(),
                today.plusDays(1).atStartOfDay(zone).toInstant()).totalMinutes();

        List<Alert> alerts = new ArrayList<>();
        financeAlerts(alerts, today);
        academicAlerts(alerts, upcoming, today);
        long overdue = todayTasks.stream().filter(TaskResponse::overdue).count();
        if (overdue > 0) {
            alerts.add(new Alert(AlertKind.TAREFA_ATRASADA, Severity.ATENCAO,
                    overdue == 1 ? "1 tarefa atrasada" : overdue + " tarefas atrasadas",
                    "Resolva ou mude o prazo", "/metas"));
        }
        for (GoalResponse g : goals.list()) {
            if (g.status() == Goal.Status.ATIVA && g.pace() == Pace.ATRASADA) {
                String expected = g.expectedPercent() == null ? "" : ", esperado " + g.expectedPercent() + "%";
                alerts.add(new Alert(AlertKind.META_ATRASADA, Severity.ATENCAO, "Meta atrasada: " + g.title(),
                        g.percent() + "% feito" + expected, "/metas/objetivos/" + g.id()));
            }
        }
        alerts.sort(Comparator.comparing(Alert::severity).thenComparing(Alert::kind));

        return new HojeResponse(today, alerts, todayTasks, classes, upcoming, nextEvents, due.size(),
                due.stream().limit(REVIEW_PREVIEW).toList(), habitsToday, metricsWithTarget,
                new Money(month.totalBalance(), month.income(), month.expense(), month.pendingCount(),
                        month.pendingAmount()),
                studyMinutes);
    }

    private void financeAlerts(List<Alert> alerts, LocalDate today) {
        OverdueBills overdue = reports.overdueBills();
        if (overdue.count() > 0) {
            alerts.add(new Alert(AlertKind.CONTA_VENCIDA, Severity.CRITICA,
                    overdue.count() == 1 ? "1 conta vencida" : overdue.count() + " contas vencidas",
                    money(overdue.amount()) + " ainda não pagos", "/financas/lancamentos"));
        }
        // a fatura que fechou (mês anterior à atual) é a que está para vencer
        for (CardResponse card : cards.list()) {
            if (card.archived()) {
                continue;
            }
            InvoiceSummary closed = cards.invoice(card.id(), card.currentInvoice().month().minusMonths(1)).summary();
            long days = ChronoUnit.DAYS.between(today, closed.dueDate());
            if (closed.status() == InvoiceStatus.ATRASADA) {
                alerts.add(new Alert(AlertKind.FATURA, Severity.CRITICA, "Fatura " + card.name() + " atrasada",
                        money(closed.remaining()) + " venceu em " + closed.dueDate().format(DAY_MONTH),
                        "/financas/contas"));
            } else if (closed.status() == InvoiceStatus.FECHADA && days <= INVOICE_WARNING_DAYS) {
                alerts.add(new Alert(AlertKind.FATURA, Severity.ATENCAO, "Fatura " + card.name() + " vence "
                        + (days == 0 ? "hoje" : days == 1 ? "amanhã" : "em " + days + " dias"),
                        money(closed.remaining()) + " a pagar", "/financas/contas"));
            }
        }
        var budgetsAtRisk = budgets.list(YearMonth.from(today)).stream()
                .filter(b -> b.status() != BudgetStatus.OK).toList();
        Map<UUID, String> categoryNames = budgetsAtRisk.isEmpty() ? Map.of()
                : categories.list().stream().collect(Collectors.toMap(CategoryResponse::id, CategoryResponse::name));
        budgetsAtRisk.forEach(b -> alerts.add(new Alert(AlertKind.ORCAMENTO,
                b.status() == BudgetStatus.ESTOURADO ? Severity.CRITICA : Severity.ATENCAO,
                "Orçamento de " + categoryNames.getOrDefault(b.categoryId(), "categoria") + " em " + b.percent() + "%",
                money(b.spent()) + " de " + money(b.limitAmount()), "/financas/orcamento")));
    }

    private void academicAlerts(List<Alert> alerts, List<AssessmentResponse> upcoming, LocalDate today) {
        for (AssessmentResponse a : upcoming) {
            long days = ChronoUnit.DAYS.between(today, a.dueOn());
            if (days <= 1 && a.score() == null) {
                alerts.add(new Alert(AlertKind.PROVA_PROXIMA, days == 0 ? Severity.CRITICA : Severity.ATENCAO,
                        (days == 0 ? "Hoje: " : "Amanhã: ") + a.title() + " de " + a.courseName(),
                        "Peso " + a.weight().stripTrailingZeros().toPlainString(),
                        "/estudos/disciplinas/" + a.courseId()));
            }
        }
        var term = terms.current();
        if (term.isEmpty()) {
            return;
        }
        for (CourseResponse c : courses.list(term.get().getId())) {
            if (c.status() != Course.Status.CURSANDO) {
                continue;
            }
            String link = "/estudos/disciplinas/" + c.id();
            if (c.absences().status() == AbsenceStatus.ESTOURADO) {
                alerts.add(new Alert(AlertKind.FALTAS, Severity.CRITICA, "Limite de faltas estourado: " + c.name(),
                        c.absences().used() + " de " + c.absences().max() + " faltas", link));
            } else if (c.absences().status() == AbsenceStatus.ATENCAO) {
                alerts.add(new Alert(AlertKind.FALTAS, Severity.ATENCAO, "Faltas perto do limite: " + c.name(),
                        c.absences().used() + " de " + c.absences().max() + " faltas", link));
            }
            if (c.grades().status() == GradeStatus.EM_RISCO) {
                alerts.add(new Alert(AlertKind.NOTA_EM_RISCO, Severity.ATENCAO, "Nota em risco: " + c.name(),
                        "Precisa de " + grade(c.grades().neededOnPending()) + " no que falta", link));
            } else if (c.grades().status() == GradeStatus.REPROVADO) {
                alerts.add(new Alert(AlertKind.NOTA_EM_RISCO, Severity.CRITICA, "Média fora de alcance: " + c.name(),
                        "Média atual " + grade(c.grades().average()), link));
            }
        }
    }

    private static String money(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(value).replace(' ', ' ');
    }

    private static String grade(BigDecimal value) {
        if (value == null) {
            return "—";
        }
        NumberFormat f = NumberFormat.getNumberInstance(PT_BR);
        f.setMinimumFractionDigits(1);
        f.setMaximumFractionDigits(2);
        return f.format(value);
    }
}
