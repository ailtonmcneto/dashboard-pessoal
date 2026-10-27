package com.vida.academico;

import com.vida.academico.AcademicDtos.ReviewRequest;
import com.vida.academico.AcademicDtos.TopicRequest;
import com.vida.academico.AcademicDtos.TopicResponse;
import com.vida.common.BusinessException;
import com.vida.common.CurrentUser;
import com.vida.common.NotFoundException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Assuntos de estudo e a revisão espaçada deles. */
@Service
public class TopicService {

    private final TopicRepository topics;
    private final CourseService courses;
    private final ResourceService resources;
    private final CurrentUser currentUser;

    TopicService(TopicRepository topics, CourseService courses, ResourceService resources, CurrentUser currentUser) {
        this.topics = topics;
        this.courses = courses;
        this.resources = resources;
        this.currentUser = currentUser;
    }

    /** Filtra por disciplina, por recurso, só os que vencem hoje, ou devolve todos. */
    @Transactional(readOnly = true)
    public List<TopicResponse> list(@Nullable UUID courseId, @Nullable UUID resourceId, boolean due) {
        UUID userId = currentUser.id();
        LocalDate today = currentUser.today();
        List<Topic> list;
        if (due) {
            list = topics.findDue(userId, today);
        } else if (courseId != null) {
            courses.find(courseId);
            list = topics.findByUserIdAndCourseIdOrderByTitle(userId, courseId);
        } else if (resourceId != null) {
            resources.find(resourceId);
            list = topics.findByUserIdAndResourceIdOrderByTitle(userId, resourceId);
        } else {
            list = topics.findByUserIdOrderByTitle(userId);
        }
        return list.stream().map(t -> toResponse(t, today)).toList();
    }

    @Transactional
    public TopicResponse create(TopicRequest req) {
        Topic topic = new Topic(currentUser.id());
        apply(topic, req);
        return toResponse(topics.save(topic), currentUser.today());
    }

    @Transactional
    public TopicResponse update(UUID id, TopicRequest req) {
        Topic topic = find(id);
        apply(topic, req);
        return toResponse(topic, currentUser.today());
    }

    /** Registra uma revisão: o domínio informado (0–5) define quando revisar de novo. */
    @Transactional
    public TopicResponse review(UUID id, ReviewRequest req) {
        Topic topic = find(id);
        LocalDate today = currentUser.today();
        topic.review(req.mastery(), today, Reviews.nextReview(req.mastery(), today));
        return toResponse(topic, today);
    }

    @Transactional
    public void delete(UUID id) {
        topics.delete(find(id));
    }

    Topic find(UUID id) {
        return topics.findByIdAndUserId(id, currentUser.id()).orElseThrow(() -> new NotFoundException("Assunto"));
    }

    Map<UUID, Topic> findAll(Collection<UUID> ids) {
        return topics.findByUserIdAndIdIn(currentUser.id(), ids).stream()
                .collect(Collectors.toMap(Topic::getId, Function.identity()));
    }

    private void apply(Topic topic, TopicRequest req) {
        if (req.courseId() != null && req.resourceId() != null) {
            throw BusinessException.badRequest("Um assunto pertence a uma disciplina ou a um curso/livro, não aos dois");
        }
        if (req.courseId() != null) {
            courses.find(req.courseId());
        }
        if (req.resourceId() != null) {
            resources.find(req.resourceId());
        }
        if (req.parentId() != null) {
            checkParent(topic, find(req.parentId()));
        }
        topic.update(req.title().strip(), req.courseId(), req.resourceId(), req.parentId());
    }

    /** O pai não pode ser o próprio assunto nem um descendente dele (sem ciclos). */
    private void checkParent(Topic topic, Topic parent) {
        Set<UUID> seen = new HashSet<>();
        for (Topic p = parent; p != null; p = p.getParentId() == null ? null : find(p.getParentId())) {
            if (topic.getId() != null && p.getId().equals(topic.getId())) {
                throw BusinessException.badRequest("Um assunto não pode estar dentro dele mesmo");
            }
            if (!seen.add(p.getId())) {
                break;
            }
        }
    }

    static TopicResponse toResponse(Topic t, LocalDate today) {
        boolean due = t.getNextReviewOn() == null || !t.getNextReviewOn().isAfter(today);
        return new TopicResponse(t.getId(), t.getTitle(), t.getCourseId(), t.getResourceId(), t.getParentId(),
                t.getMastery(), t.getLastReviewedOn(), t.getNextReviewOn(), due);
    }
}
