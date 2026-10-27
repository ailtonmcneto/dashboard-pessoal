package com.vida.metas;

import com.vida.common.BusinessException;
import com.vida.common.CurrentUser;
import com.vida.common.NotFoundException;
import com.vida.common.Strings;
import com.vida.financas.AccountService;
import com.vida.metas.GoalDtos.GoalRequest;
import com.vida.metas.GoalDtos.GoalResponse;
import com.vida.metas.GoalDtos.MilestoneRequest;
import com.vida.metas.GoalDtos.MilestoneResponse;
import com.vida.metas.GoalDtos.Pace;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Metas e os marcos delas. */
@Service
public class GoalService {

    private final GoalRepository goals;
    private final MilestoneRepository milestones;
    private final TaskRepository tasks;
    private final AccountService accounts;
    private final CurrentUser currentUser;

    GoalService(GoalRepository goals, MilestoneRepository milestones, TaskRepository tasks, AccountService accounts,
            CurrentUser currentUser) {
        this.goals = goals;
        this.milestones = milestones;
        this.tasks = tasks;
        this.accounts = accounts;
        this.currentUser = currentUser;
    }

    /** Metas com progresso, ritmo, marcos e contagem de tarefas. Ativas primeiro. */
    @Transactional(readOnly = true)
    public List<GoalResponse> list() {
        return toResponses(goals.findByUserIdOrderByStatusAscDueOnAscTitleAsc(currentUser.id()));
    }

    @Transactional(readOnly = true)
    public GoalResponse get(UUID id) {
        return toResponses(List.of(find(id))).getFirst();
    }

    @Transactional
    public GoalResponse create(GoalRequest req) {
        Goal goal = new Goal(currentUser.id());
        apply(goal, req);
        goal.setStatus(Goal.Status.ATIVA, currentUser.today());
        goals.save(goal);
        return get(goal.getId());
    }

    @Transactional
    public GoalResponse update(UUID id, GoalRequest req) {
        apply(find(id), req);
        return get(id);
    }

    @Transactional
    public GoalResponse setStatus(UUID id, Goal.Status status) {
        find(id).setStatus(status, currentUser.today());
        return get(id);
    }

    /** Apaga a meta e os marcos; tarefas e projetos ligados ficam, sem o vínculo. */
    @Transactional
    public void delete(UUID id) {
        goals.delete(find(id));
    }

    // ---------- marcos ----------

    @Transactional
    public GoalResponse addMilestone(UUID goalId, MilestoneRequest req) {
        Goal goal = find(goalId);
        Milestone m = new Milestone(currentUser.id(), goal.getId(), milestones.countByGoalId(goalId));
        m.update(req.title().strip(), req.dueOn());
        milestones.save(m);
        return get(goalId);
    }

    @Transactional
    public GoalResponse updateMilestone(UUID id, MilestoneRequest req) {
        Milestone m = findMilestone(id);
        m.update(req.title().strip(), req.dueOn());
        return get(m.getGoalId());
    }

    @Transactional
    public GoalResponse setMilestoneDone(UUID id, boolean done) {
        Milestone m = findMilestone(id);
        m.setDoneOn(done ? (m.getDoneOn() != null ? m.getDoneOn() : currentUser.today()) : null);
        return get(m.getGoalId());
    }

    @Transactional
    public GoalResponse deleteMilestone(UUID id) {
        Milestone m = findMilestone(id);
        milestones.delete(m);
        return get(m.getGoalId());
    }

    Goal find(UUID id) {
        return goals.findByIdAndUserId(id, currentUser.id()).orElseThrow(() -> new NotFoundException("Meta"));
    }

    private Milestone findMilestone(UUID id) {
        return milestones.findByIdAndUserId(id, currentUser.id()).orElseThrow(() -> new NotFoundException("Marco"));
    }

    private void apply(Goal goal, GoalRequest req) {
        Goal.ProgressMode mode = req.progressMode() == null ? Goal.ProgressMode.MARCOS : req.progressMode();
        if (req.startsOn() != null && req.dueOn() != null && req.dueOn().isBefore(req.startsOn())) {
            throw BusinessException.badRequest("O prazo não pode ser antes do início");
        }
        if ((mode == Goal.ProgressMode.MANUAL || mode == Goal.ProgressMode.FINANCEIRO) && req.targetValue() == null) {
            throw BusinessException.badRequest("Informe o valor-alvo da meta");
        }
        UUID accountId = null;
        if (mode == Goal.ProgressMode.FINANCEIRO) {
            if (req.accountId() == null) {
                throw BusinessException.badRequest("Meta financeira precisa de uma conta");
            }
            accounts.requireOwned(req.accountId());
            accountId = req.accountId();
        }
        boolean usesTarget = mode == Goal.ProgressMode.MANUAL || mode == Goal.ProgressMode.FINANCEIRO;
        LocalDate startsOn = req.startsOn() != null ? req.startsOn()
                : goal.getStartsOn() != null ? goal.getStartsOn() : currentUser.today();
        goal.update(req.title().strip(), Strings.clean(req.description()),
                req.area() == null ? Goal.Area.PESSOAL : req.area(), startsOn, req.dueOn(), mode,
                usesTarget ? req.targetValue() : null,
                mode == Goal.ProgressMode.MANUAL ? (req.currentValue() == null ? BigDecimal.ZERO : req.currentValue())
                        : null,
                usesTarget ? Strings.clean(req.unit()) : null, accountId);
    }

    private List<GoalResponse> toResponses(List<Goal> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        UUID userId = currentUser.id();
        LocalDate today = currentUser.today();
        Map<UUID, List<Milestone>> milestonesByGoal = milestones
                .findByUserIdAndGoalIdInOrderByPositionAscCreatedAtAsc(userId, list.stream().map(Goal::getId).toList())
                .stream().collect(Collectors.groupingBy(Milestone::getGoalId));
        Map<UUID, List<Task>> tasksByGoal = tasks.findByUserId(userId).stream()
                .filter(t -> t.getGoalId() != null)
                .collect(Collectors.groupingBy(Task::getGoalId));
        boolean needsBalances = list.stream().anyMatch(g -> g.getProgressMode() == Goal.ProgressMode.FINANCEIRO);
        Map<UUID, BigDecimal> balances = needsBalances ? accounts.paidBalances() : Map.of();

        return list.stream().map(g -> {
            List<Milestone> ms = milestonesByGoal.getOrDefault(g.getId(), List.of());
            List<Task> ts = tasksByGoal.getOrDefault(g.getId(), List.of());
            int doneTasks = (int) ts.stream().filter(Task::isDone).count();
            BigDecimal current;
            BigDecimal target;
            switch (g.getProgressMode()) {
                case MANUAL -> {
                    current = g.getCurrentValue();
                    target = g.getTargetValue();
                }
                case MARCOS -> {
                    current = BigDecimal.valueOf(ms.stream().filter(m -> m.getDoneOn() != null).count());
                    target = BigDecimal.valueOf(ms.size());
                }
                case TAREFAS -> {
                    current = BigDecimal.valueOf(doneTasks);
                    target = BigDecimal.valueOf(ts.size());
                }
                default -> {
                    current = g.getAccountId() == null ? BigDecimal.ZERO
                            : balances.getOrDefault(g.getAccountId(), BigDecimal.ZERO);
                    target = g.getTargetValue();
                }
            }
            int percent = GoalProgress.percent(current, target);
            Integer expected = GoalProgress.expected(g.getStartsOn(), g.getDueOn(), today);
            Pace pace = GoalProgress.pace(g.getStatus(), percent, expected, g.getDueOn(), today);
            String accountName = g.getAccountId() == null ? null : accounts.requireOwned(g.getAccountId());
            List<MilestoneResponse> milestoneResponses = ms.stream()
                    .map(m -> new MilestoneResponse(m.getId(), m.getTitle(), m.getDueOn(), m.getDoneOn(),
                            m.getDoneOn() != null, m.getPosition()))
                    .toList();
            return new GoalResponse(g.getId(), g.getTitle(), g.getDescription(), g.getArea(), g.getStatus(),
                    g.getStartsOn(), g.getDueOn(), g.getCompletedOn(), g.getProgressMode(), g.getTargetValue(),
                    g.getCurrentValue(), g.getUnit(), g.getAccountId(), accountName, current, target, percent,
                    expected, pace, milestoneResponses, ts.size() - doneTasks, doneTasks);
        }).toList();
    }
}
