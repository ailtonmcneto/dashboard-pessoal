package com.vida.academico;

import com.vida.academico.AcademicDtos.StudyBucket;
import com.vida.academico.AcademicDtos.StudyDay;
import com.vida.academico.AcademicDtos.StudySessionRequest;
import com.vida.academico.AcademicDtos.StudySessionResponse;
import com.vida.academico.AcademicDtos.StudySummary;
import com.vida.common.BusinessException;
import com.vida.common.CurrentUser;
import com.vida.common.NotFoundException;
import com.vida.common.Strings;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudySessionService {

    static final Duration MAX_RANGE = Duration.ofDays(366);
    static final String NO_SUBJECT = "Sem vínculo";

    private final StudySessionRepository sessions;
    private final CourseService courses;
    private final ResourceService resources;
    private final TopicService topics;
    private final CurrentUser currentUser;

    StudySessionService(StudySessionRepository sessions, CourseService courses, ResourceService resources,
            TopicService topics, CurrentUser currentUser) {
        this.sessions = sessions;
        this.courses = courses;
        this.resources = resources;
        this.topics = topics;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<StudySessionResponse> list(Instant from, Instant to) {
        List<StudySession> list = load(from, to);
        Labels labels = labels(list);
        return list.stream().map(s -> toResponse(s, labels)).toList();
    }

    /** Total, por disciplina/curso e por dia (no fuso do usuário) entre [from, to). */
    @Transactional(readOnly = true)
    public StudySummary summary(Instant from, Instant to) {
        List<StudySession> list = load(from, to);
        Labels labels = labels(list);
        ZoneId zone = currentUser.zone();

        Map<Subject, Integer> bySubject = new LinkedHashMap<>();
        for (StudySession s : list) {
            bySubject.merge(new Subject(s.getCourseId(), s.getResourceId()), s.getMinutes(), Integer::sum);
        }
        List<StudyBucket> buckets = bySubject.entrySet().stream()
                .map(e -> {
                    UUID courseId = e.getKey().courseId();
                    UUID resourceId = e.getKey().resourceId();
                    Course c = courseId == null ? null : labels.courses().get(courseId);
                    Resource r = resourceId == null ? null : labels.resources().get(resourceId);
                    String label = c != null ? c.getName() : r != null ? r.getTitle() : NO_SUBJECT;
                    return new StudyBucket(courseId, resourceId, label, c == null ? null : c.getColor(), e.getValue());
                })
                .sorted(Comparator.comparingInt(StudyBucket::minutes).reversed())
                .toList();

        // todos os dias do intervalo, inclusive os sem estudo (bom para gráfico)
        Map<LocalDate, Integer> byDay = new LinkedHashMap<>();
        LocalDate first = from.atZone(zone).toLocalDate();
        LocalDate last = to.minusNanos(1).atZone(zone).toLocalDate();
        for (LocalDate d = first; !d.isAfter(last); d = d.plusDays(1)) {
            byDay.put(d, 0);
        }
        list.forEach(s -> byDay.merge(s.getStartedAt().atZone(zone).toLocalDate(), s.getMinutes(), Integer::sum));

        int total = list.stream().mapToInt(StudySession::getMinutes).sum();
        List<StudyDay> days = new ArrayList<>();
        byDay.forEach((d, m) -> days.add(new StudyDay(d, m)));
        return new StudySummary(total, buckets, days);
    }

    @Transactional
    public StudySessionResponse create(StudySessionRequest req) {
        if (req.courseId() != null && req.resourceId() != null) {
            throw BusinessException.badRequest("Escolha uma disciplina ou um curso/livro, não os dois");
        }
        if (req.startedAt().isAfter(Instant.now().plus(Duration.ofMinutes(5)))) {
            throw BusinessException.badRequest("A sessão não pode começar no futuro");
        }
        UUID courseId = req.courseId();
        UUID resourceId = req.resourceId();
        if (courseId != null) {
            courses.find(courseId);
        }
        if (resourceId != null) {
            resources.find(resourceId);
        }
        if (req.topicId() != null) {
            Topic topic = topics.find(req.topicId());
            // sem vínculo explícito, herda o do assunto
            if (courseId == null && resourceId == null) {
                courseId = topic.getCourseId();
                resourceId = topic.getResourceId();
            }
        }
        StudySession session = sessions.save(new StudySession(currentUser.id(), req.startedAt(), req.minutes(),
                courseId, resourceId, req.topicId(), Strings.clean(req.notes())));
        return toResponse(session, labels(List.of(session)));
    }

    @Transactional
    public void delete(UUID id) {
        sessions.delete(sessions.findByIdAndUserId(id, currentUser.id())
                .orElseThrow(() -> new NotFoundException("Sessão de estudo")));
    }

    private List<StudySession> load(Instant from, Instant to) {
        if (!to.isAfter(from)) {
            throw BusinessException.badRequest("'to' precisa ser depois de 'from'");
        }
        if (Duration.between(from, to).compareTo(MAX_RANGE) > 0) {
            throw BusinessException.badRequest("Intervalo máximo de 366 dias");
        }
        return sessions.findByUserIdAndStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtDesc(
                currentUser.id(), from, to);
    }

    private record Subject(@Nullable UUID courseId, @Nullable UUID resourceId) {
    }

    private record Labels(Map<UUID, Course> courses, Map<UUID, Resource> resources, Map<UUID, Topic> topics) {
    }

    private Labels labels(List<StudySession> list) {
        var courseIds = list.stream().map(StudySession::getCourseId).filter(Objects::nonNull).collect(Collectors.toSet());
        var resourceIds = list.stream().map(StudySession::getResourceId).filter(Objects::nonNull).collect(Collectors.toSet());
        var topicIds = list.stream().map(StudySession::getTopicId).filter(Objects::nonNull).collect(Collectors.toSet());
        return new Labels(courseIds.isEmpty() ? Map.of() : courses.findAll(courseIds),
                resourceIds.isEmpty() ? Map.of() : resources.findAll(resourceIds),
                topicIds.isEmpty() ? Map.of() : topics.findAll(topicIds));
    }

    private static StudySessionResponse toResponse(StudySession s, Labels labels) {
        String label = Stream.of(
                        s.getCourseId() == null ? null : labels.courses().get(s.getCourseId()).getName(),
                        s.getResourceId() == null ? null : labels.resources().get(s.getResourceId()).getTitle(),
                        s.getTopicId() == null ? null : labels.topics().get(s.getTopicId()).getTitle())
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" · "));
        return new StudySessionResponse(s.getId(), s.getStartedAt(), s.getMinutes(), s.getCourseId(),
                s.getResourceId(), s.getTopicId(), s.getNotes(), label.isEmpty() ? NO_SUBJECT : label);
    }
}
