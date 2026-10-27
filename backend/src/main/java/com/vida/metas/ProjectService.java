package com.vida.metas;

import com.vida.academico.CourseService;
import com.vida.common.BusinessException;
import com.vida.common.CurrentUser;
import com.vida.common.NotFoundException;
import com.vida.common.Strings;
import com.vida.metas.GoalDtos.ProjectRequest;
import com.vida.metas.GoalDtos.ProjectResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

    private final ProjectRepository projects;
    private final GoalRepository goals;
    private final TaskRepository tasks;
    private final CourseService courses;
    private final CurrentUser currentUser;

    ProjectService(ProjectRepository projects, GoalRepository goals, TaskRepository tasks, CourseService courses,
            CurrentUser currentUser) {
        this.projects = projects;
        this.goals = goals;
        this.tasks = tasks;
        this.courses = courses;
        this.currentUser = currentUser;
    }

    /** Projetos com progresso pelas tarefas. Ativos primeiro. */
    @Transactional(readOnly = true)
    public List<ProjectResponse> list() {
        return toResponses(projects.findByUserIdOrderByStatusAscDueOnAscTitleAsc(currentUser.id()));
    }

    @Transactional
    public ProjectResponse create(ProjectRequest req) {
        Project project = new Project(currentUser.id());
        apply(project, req);
        return toResponses(List.of(projects.save(project))).getFirst();
    }

    @Transactional
    public ProjectResponse update(UUID id, ProjectRequest req) {
        Project project = find(id);
        apply(project, req);
        return toResponses(List.of(project)).getFirst();
    }

    /** Apaga o projeto; as tarefas dele ficam, sem o vínculo. */
    @Transactional
    public void delete(UUID id) {
        projects.delete(find(id));
    }

    private Project find(UUID id) {
        return projects.findByIdAndUserId(id, currentUser.id()).orElseThrow(() -> new NotFoundException("Projeto"));
    }

    private void apply(Project project, ProjectRequest req) {
        if (req.startsOn() != null && req.dueOn() != null && req.dueOn().isBefore(req.startsOn())) {
            throw BusinessException.badRequest("O prazo não pode ser antes do início");
        }
        if (req.goalId() != null) {
            goals.findByIdAndUserId(req.goalId(), currentUser.id()).orElseThrow(() -> new NotFoundException("Meta"));
        }
        if (req.courseId() != null) {
            courses.requireOwned(req.courseId());
        }
        project.update(req.title().strip(), Strings.clean(req.description()),
                req.kind() == null ? Project.Kind.PESSOAL : req.kind(),
                req.status() == null ? Project.Status.ATIVO : req.status(), req.startsOn(), req.dueOn(),
                Strings.clean(req.url()), req.goalId(), req.courseId());
    }

    private List<ProjectResponse> toResponses(List<Project> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        UUID userId = currentUser.id();
        LocalDate today = currentUser.today();
        Map<UUID, List<Task>> tasksByProject = tasks.findByUserId(userId).stream()
                .filter(t -> t.getProjectId() != null)
                .collect(Collectors.groupingBy(Task::getProjectId));
        Map<UUID, String> goalTitles = goals.findByUserIdAndIdIn(userId,
                        list.stream().map(Project::getGoalId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(Goal::getId, Goal::getTitle));
        Map<UUID, String> courseNames = courses.names(
                list.stream().map(Project::getCourseId).filter(Objects::nonNull).distinct().toList());
        return list.stream().map(p -> {
            List<Task> ts = tasksByProject.getOrDefault(p.getId(), List.of());
            int done = (int) ts.stream().filter(Task::isDone).count();
            int percent = ts.isEmpty() ? 0 : done * 100 / ts.size();
            boolean overdue = p.getStatus() != Project.Status.CONCLUIDO && p.getDueOn() != null
                    && p.getDueOn().isBefore(today);
            return new ProjectResponse(p.getId(), p.getTitle(), p.getDescription(), p.getKind(), p.getStatus(),
                    p.getStartsOn(), p.getDueOn(), p.getUrl(),
                    p.getGoalId(), TaskService.name(goalTitles, p.getGoalId()),
                    p.getCourseId(), TaskService.name(courseNames, p.getCourseId()),
                    ts.size() - done, done, percent, overdue);
        }).toList();
    }
}
