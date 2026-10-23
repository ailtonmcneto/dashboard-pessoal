package com.vida.academico;

import com.vida.academico.AcademicDtos.ResourceRequest;
import com.vida.academico.AcademicDtos.ResourceResponse;
import com.vida.academico.AcademicDtos.ReviewRequest;
import com.vida.academico.AcademicDtos.StudySessionRequest;
import com.vida.academico.AcademicDtos.StudySessionResponse;
import com.vida.academico.AcademicDtos.StudySummary;
import com.vida.academico.AcademicDtos.TopicRequest;
import com.vida.academico.AcademicDtos.TopicResponse;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

/** Estudo além da grade: cursos/livros, assuntos com revisão e sessões de estudo. */
@RestController
class StudyController {

    private final ResourceService resources;
    private final TopicService topics;
    private final StudySessionService sessions;

    StudyController(ResourceService resources, TopicService topics, StudySessionService sessions) {
        this.resources = resources;
        this.topics = topics;
        this.sessions = sessions;
    }

    // ---------- Cursos livres, livros… ----------

    @GetMapping("/resources")
    List<ResourceResponse> resources() {
        return resources.list();
    }

    @PostMapping("/resources")
    @ResponseStatus(HttpStatus.CREATED)
    ResourceResponse createResource(@Valid @RequestBody ResourceRequest req) {
        return resources.create(req);
    }

    @PutMapping("/resources/{id}")
    ResourceResponse updateResource(@PathVariable UUID id, @Valid @RequestBody ResourceRequest req) {
        return resources.update(id, req);
    }

    @DeleteMapping("/resources/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteResource(@PathVariable UUID id) {
        resources.delete(id);
    }

    // ---------- Assuntos ----------

    /** {@code ?courseId=}, {@code ?resourceId=}, {@code ?due=true} (para revisar hoje) ou todos. */
    @GetMapping("/topics")
    List<TopicResponse> topics(@RequestParam(required = false) @Nullable UUID courseId,
            @RequestParam(required = false) @Nullable UUID resourceId,
            @RequestParam(defaultValue = "false") boolean due) {
        return topics.list(courseId, resourceId, due);
    }

    @PostMapping("/topics")
    @ResponseStatus(HttpStatus.CREATED)
    TopicResponse createTopic(@Valid @RequestBody TopicRequest req) {
        return topics.create(req);
    }

    @PutMapping("/topics/{id}")
    TopicResponse updateTopic(@PathVariable UUID id, @Valid @RequestBody TopicRequest req) {
        return topics.update(id, req);
    }

    @PostMapping("/topics/{id}/review")
    TopicResponse review(@PathVariable UUID id, @Valid @RequestBody ReviewRequest req) {
        return topics.review(id, req);
    }

    @DeleteMapping("/topics/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteTopic(@PathVariable UUID id) {
        topics.delete(id);
    }

    // ---------- Sessões de estudo ----------

    /** Sem parâmetros: últimos 7 dias. */
    @GetMapping("/study-sessions")
    List<StudySessionResponse> sessions(@RequestParam(required = false) @Nullable Instant from,
            @RequestParam(required = false) @Nullable Instant to) {
        Instant end = to != null ? to : Instant.now();
        return sessions.list(from != null ? from : end.minus(7, ChronoUnit.DAYS), end);
    }

    @GetMapping("/study-sessions/summary")
    StudySummary summary(@RequestParam Instant from, @RequestParam Instant to) {
        return sessions.summary(from, to);
    }

    @PostMapping("/study-sessions")
    @ResponseStatus(HttpStatus.CREATED)
    StudySessionResponse createSession(@Valid @RequestBody StudySessionRequest req) {
        return sessions.create(req);
    }

    @DeleteMapping("/study-sessions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteSession(@PathVariable UUID id) {
        sessions.delete(id);
    }
}
