package com.vida.metas;

import com.vida.metas.GoalDtos.GoalRequest;
import com.vida.metas.GoalDtos.GoalResponse;
import com.vida.metas.GoalDtos.MilestoneRequest;
import com.vida.metas.GoalDtos.ProjectRequest;
import com.vida.metas.GoalDtos.ProjectResponse;
import com.vida.metas.GoalDtos.StatusRequest;
import com.vida.metas.GoalDtos.TaskRequest;
import com.vida.metas.GoalDtos.TaskResponse;
import com.vida.metas.GoalDtos.TaskView;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Metas (com marcos), projetos e a lista única de tarefas. */
@RestController
class GoalController {

    private final GoalService goals;
    private final ProjectService projects;
    private final TaskService tasks;

    GoalController(GoalService goals, ProjectService projects, TaskService tasks) {
        this.goals = goals;
        this.projects = projects;
        this.tasks = tasks;
    }

    // ---------- Metas ----------

    @GetMapping("/goals")
    List<GoalResponse> goals() {
        return goals.list();
    }

    @GetMapping("/goals/{id}")
    GoalResponse goal(@PathVariable UUID id) {
        return goals.get(id);
    }

    @PostMapping("/goals")
    @ResponseStatus(HttpStatus.CREATED)
    GoalResponse createGoal(@Valid @RequestBody GoalRequest req) {
        return goals.create(req);
    }

    @PutMapping("/goals/{id}")
    GoalResponse updateGoal(@PathVariable UUID id, @Valid @RequestBody GoalRequest req) {
        return goals.update(id, req);
    }

    /** Concluir, abandonar ou reabrir. */
    @PutMapping("/goals/{id}/status")
    GoalResponse setGoalStatus(@PathVariable UUID id, @Valid @RequestBody StatusRequest req) {
        return goals.setStatus(id, req.status());
    }

    @DeleteMapping("/goals/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteGoal(@PathVariable UUID id) {
        goals.delete(id);
    }

    @PostMapping("/goals/{id}/milestones")
    @ResponseStatus(HttpStatus.CREATED)
    GoalResponse addMilestone(@PathVariable UUID id, @Valid @RequestBody MilestoneRequest req) {
        return goals.addMilestone(id, req);
    }

    @PutMapping("/milestones/{id}")
    GoalResponse updateMilestone(@PathVariable UUID id, @Valid @RequestBody MilestoneRequest req) {
        return goals.updateMilestone(id, req);
    }

    @PostMapping("/milestones/{id}/done")
    GoalResponse milestoneDone(@PathVariable UUID id) {
        return goals.setMilestoneDone(id, true);
    }

    @PostMapping("/milestones/{id}/undone")
    GoalResponse milestoneUndone(@PathVariable UUID id) {
        return goals.setMilestoneDone(id, false);
    }

    @DeleteMapping("/milestones/{id}")
    GoalResponse deleteMilestone(@PathVariable UUID id) {
        return goals.deleteMilestone(id);
    }

    // ---------- Projetos ----------

    @GetMapping("/projects")
    List<ProjectResponse> projects() {
        return projects.list();
    }

    @PostMapping("/projects")
    @ResponseStatus(HttpStatus.CREATED)
    ProjectResponse createProject(@Valid @RequestBody ProjectRequest req) {
        return projects.create(req);
    }

    @PutMapping("/projects/{id}")
    ProjectResponse updateProject(@PathVariable UUID id, @Valid @RequestBody ProjectRequest req) {
        return projects.update(id, req);
    }

    @DeleteMapping("/projects/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteProject(@PathVariable UUID id) {
        projects.delete(id);
    }

    // ---------- Tarefas ----------

    /** {@code view}: HOJE, SEMANA, ABERTAS (padrão) ou FEITAS; filtros opcionais por meta, projeto, disciplina. */
    @GetMapping("/tasks")
    List<TaskResponse> tasks(@RequestParam(defaultValue = "ABERTAS") TaskView view,
            @RequestParam(required = false) @Nullable UUID goalId,
            @RequestParam(required = false) @Nullable UUID projectId,
            @RequestParam(required = false) @Nullable UUID courseId) {
        return tasks.list(view, goalId, projectId, courseId);
    }

    @PostMapping("/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    TaskResponse createTask(@Valid @RequestBody TaskRequest req) {
        return tasks.create(req);
    }

    @PutMapping("/tasks/{id}")
    TaskResponse updateTask(@PathVariable UUID id, @Valid @RequestBody TaskRequest req) {
        return tasks.update(id, req);
    }

    @PostMapping("/tasks/{id}/done")
    TaskResponse taskDone(@PathVariable UUID id) {
        return tasks.setDone(id, true);
    }

    @PostMapping("/tasks/{id}/undone")
    TaskResponse taskUndone(@PathVariable UUID id) {
        return tasks.setDone(id, false);
    }

    @DeleteMapping("/tasks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteTask(@PathVariable UUID id) {
        tasks.delete(id);
    }
}
