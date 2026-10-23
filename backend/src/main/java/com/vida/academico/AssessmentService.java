package com.vida.academico;

import com.vida.academico.AcademicDtos.AssessmentRequest;
import com.vida.academico.AcademicDtos.AssessmentResponse;
import com.vida.common.BusinessException;
import com.vida.common.CurrentUser;
import com.vida.common.NotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssessmentService {

    private final AssessmentRepository assessments;
    private final TopicRepository topics;
    private final CourseService courses;
    private final CurrentUser currentUser;

    AssessmentService(AssessmentRepository assessments, TopicRepository topics, CourseService courses,
            CurrentUser currentUser) {
        this.assessments = assessments;
        this.topics = topics;
        this.courses = courses;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<AssessmentResponse> list(UUID courseId) {
        Course course = courses.find(courseId);
        return assessments.findByUserIdAndCourseIdOrderByDueOnAscTitleAsc(currentUser.id(), courseId).stream()
                .map(a -> toResponse(a, course))
                .toList();
    }

    /** Avaliações com data de hoje até {@code days} dias à frente — para a tela Hoje. */
    @Transactional(readOnly = true)
    public List<AssessmentResponse> upcoming(int days) {
        if (days < 0 || days > 180) {
            throw BusinessException.badRequest("days deve estar entre 0 e 180");
        }
        LocalDate today = currentUser.today();
        List<Assessment> list = assessments.findByUserIdAndDueOnBetweenOrderByDueOn(
                currentUser.id(), today, today.plusDays(days));
        Map<UUID, Course> byId = courses.findAll(list.stream().map(Assessment::getCourseId).collect(Collectors.toSet()));
        return list.stream()
                .filter(a -> byId.get(a.getCourseId()).getStatus() == Course.Status.CURSANDO)
                .map(a -> toResponse(a, byId.get(a.getCourseId())))
                .toList();
    }

    @Transactional
    public AssessmentResponse create(UUID courseId, AssessmentRequest req) {
        Course course = courses.find(courseId);
        Assessment assessment = new Assessment(currentUser.id(), course.getId());
        apply(assessment, req);
        return toResponse(assessments.save(assessment), course);
    }

    @Transactional
    public AssessmentResponse update(UUID id, AssessmentRequest req) {
        Assessment assessment = find(id);
        apply(assessment, req);
        return toResponse(assessment, courses.find(assessment.getCourseId()));
    }

    @Transactional
    public void delete(UUID id) {
        assessments.delete(find(id));
    }

    private Assessment find(UUID id) {
        return assessments.findByIdAndUserId(id, currentUser.id())
                .orElseThrow(() -> new NotFoundException("Avaliação"));
    }

    private void apply(Assessment assessment, AssessmentRequest req) {
        BigDecimal maxScore = req.maxScore() == null ? BigDecimal.TEN : req.maxScore();
        if (req.score() != null && req.score().compareTo(maxScore) > 0) {
            throw BusinessException.badRequest("A nota não pode passar da nota máxima (" + maxScore + ")");
        }
        Set<UUID> topicIds = req.topicIds() == null ? Set.of() : req.topicIds();
        if (!topicIds.isEmpty() && topics.findByUserIdAndIdIn(currentUser.id(), topicIds).size() != topicIds.size()) {
            throw new NotFoundException("Assunto");
        }
        assessment.update(req.title().strip(), req.kind() == null ? Assessment.Kind.PROVA : req.kind(), req.dueOn(),
                req.weight() == null ? BigDecimal.ONE : req.weight(), maxScore, req.score(), topicIds);
    }

    static AssessmentResponse toResponse(Assessment a, Course c) {
        BigDecimal normalized = a.getScore() == null ? null
                : a.getScore().multiply(BigDecimal.TEN).divide(a.getMaxScore(), 2, RoundingMode.HALF_UP);
        return new AssessmentResponse(a.getId(), c.getId(), c.getName(), c.getColor(), a.getTitle(), a.getKind(),
                a.getDueOn(), a.getWeight(), a.getMaxScore(), a.getScore(), normalized, a.getTopicIds());
    }
}
