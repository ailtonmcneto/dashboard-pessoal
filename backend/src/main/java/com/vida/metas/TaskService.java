package com.vida.metas;

import com.vida.academico.CourseService;
import com.vida.common.BusinessException;
import com.vida.common.CurrentUser;
import com.vida.common.NotFoundException;
import com.vida.common.Strings;
import com.vida.metas.GoalDtos.TaskRequest;
import com.vida.metas.GoalDtos.TaskResponse;
import com.vida.metas.GoalDtos.TaskView;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    /** Abertas: vencidas primeiro, depois por prazo (sem prazo no fim) e prioridade. */
    static final Comparator<Task> OPEN_ORDER = Comparator
            .comparing(Task::getDueOn, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparingInt(Task::getPriority)
            .thenComparing(Task::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));

    private final TaskRepository tasks;
    private final GoalRepository goals;
    private final MilestoneRepository milestones;
    private final ProjectRepository projects;
    private final CourseService courses;
    private final CurrentUser currentUser;

    TaskService(TaskRepository tasks, GoalRepository goals, MilestoneRepository milestones,
            ProjectRepository projects, CourseService courses, CurrentUser currentUser) {
        this.tasks = tasks;
        this.goals = goals;
        this.milestones = milestones;
        this.projects = projects;
        this.courses = courses;
        this.currentUser = currentUser;
    }

    /** Filtra por visão e, opcionalmente, por meta, projeto ou disciplina. */
    @Transactional(readOnly = true)
    public List<TaskResponse> list(TaskView view, @Nullable UUID goalId, @Nullable UUID projectId,
            @Nullable UUID courseId) {
        UUID userId = currentUser.id();
        LocalDate today = currentUser.today();
        List<Task> list = view == TaskView.FEITAS
                ? tasks.findByUserIdAndDoneAtGreaterThanEqualOrderByDoneAtDesc(userId,
                        today.minusDays(30).atStartOfDay(currentUser.zone()).toInstant())
                : tasks.findByUserIdAndDoneAtIsNull(userId).stream()
                        .filter(t -> switch (view) {
                            case HOJE -> t.getDueOn() != null && !t.getDueOn().isAfter(today);
                            case SEMANA -> t.getDueOn() != null && !t.getDueOn().isAfter(today.plusDays(6));
                            default -> true;
                        })
                        .sorted(OPEN_ORDER)
                        .toList();
        List<Task> filtered = list.stream()
                .filter(t -> goalId == null || goalId.equals(t.getGoalId()))
                .filter(t -> projectId == null || projectId.equals(t.getProjectId()))
                .filter(t -> courseId == null || courseId.equals(t.getCourseId()))
                .toList();
        return toResponses(filtered, today);
    }

    @Transactional
    public TaskResponse create(TaskRequest req) {
        Task task = new Task(currentUser.id());
        apply(task, req);
        return toResponses(List.of(tasks.save(task)), currentUser.today()).getFirst();
    }

    @Transactional
    public TaskResponse update(UUID id, TaskRequest req) {
        Task task = find(id);
        apply(task, req);
        return toResponses(List.of(task), currentUser.today()).getFirst();
    }

    @Transactional
    public TaskResponse setDone(UUID id, boolean done) {
        Task task = find(id);
        if (done != task.isDone()) {
            task.setDoneAt(done ? Instant.now() : null);
        }
        return toResponses(List.of(task), currentUser.today()).getFirst();
    }

    @Transactional
    public void delete(UUID id) {
        tasks.delete(find(id));
    }

    private Task find(UUID id) {
        return tasks.findByIdAndUserId(id, currentUser.id()).orElseThrow(() -> new NotFoundException("Tarefa"));
    }

    private void apply(Task task, TaskRequest req) {
        UUID userId = currentUser.id();
        UUID goalId = req.goalId();
        if (req.milestoneId() != null) {
            Milestone m = milestones.findByIdAndUserId(req.milestoneId(), userId)
                    .orElseThrow(() -> new NotFoundException("Marco"));
            if (goalId != null && !goalId.equals(m.getGoalId())) {
                throw BusinessException.badRequest("O marco é de outra meta");
            }
            goalId = m.getGoalId(); // tarefa de um marco conta para a meta dele
        }
        if (goalId != null) {
            goals.findByIdAndUserId(goalId, userId).orElseThrow(() -> new NotFoundException("Meta"));
        }
        if (req.projectId() != null) {
            projects.findByIdAndUserId(req.projectId(), userId).orElseThrow(() -> new NotFoundException("Projeto"));
        }
        if (req.courseId() != null) {
            courses.requireOwned(req.courseId());
        }
        task.update(req.title().strip(), Strings.clean(req.notes()), req.priority() == null ? 2 : req.priority(),
                req.dueOn(), goalId, req.milestoneId(), req.projectId(), req.courseId());
    }

    /** Monta as respostas com os nomes de meta, projeto e disciplina, buscados de uma vez. */
    private List<TaskResponse> toResponses(List<Task> list, LocalDate today) {
        UUID userId = currentUser.id();
        Map<UUID, String> goalTitles = goals.findByUserIdAndIdIn(userId, ids(list, Task::getGoalId)).stream()
                .collect(Collectors.toMap(Goal::getId, Goal::getTitle));
        Map<UUID, String> projectTitles = projects.findByUserIdAndIdIn(userId, ids(list, Task::getProjectId)).stream()
                .collect(Collectors.toMap(Project::getId, Project::getTitle));
        Map<UUID, String> courseNames = courses.names(ids(list, Task::getCourseId));
        return list.stream().map(t -> new TaskResponse(t.getId(), t.getTitle(), t.getNotes(), t.getPriority(),
                t.getDueOn(), t.isDone(), t.getDoneAt(),
                !t.isDone() && t.getDueOn() != null && t.getDueOn().isBefore(today),
                t.getGoalId(), name(goalTitles, t.getGoalId()), t.getMilestoneId(),
                t.getProjectId(), name(projectTitles, t.getProjectId()),
                t.getCourseId(), name(courseNames, t.getCourseId()))).toList();
    }

    /** Nome pelo id; tolera id nulo (mapas imutáveis não aceitam get(null)). */
    static @Nullable String name(Map<UUID, String> names, @Nullable UUID id) {
        return id == null ? null : names.get(id);
    }

    private static List<UUID> ids(List<Task> list, Function<Task, UUID> getter) {
        return list.stream().map(getter).filter(Objects::nonNull).distinct().toList();
    }
}
