package com.vida.academico;

import com.vida.academico.AcademicDtos.AbsenceRequest;
import com.vida.academico.AcademicDtos.AbsenceResponse;
import com.vida.academico.AcademicDtos.AbsenceStatus;
import com.vida.academico.AcademicDtos.AbsenceSummary;
import com.vida.academico.AcademicDtos.CourseRequest;
import com.vida.academico.AcademicDtos.CourseResponse;
import com.vida.academico.AcademicDtos.GradeStatus;
import com.vida.academico.AcademicDtos.GradeSummary;
import com.vida.academico.AcademicDtos.SlotRequest;
import com.vida.academico.AcademicDtos.SlotResponse;
import com.vida.common.BusinessException;
import com.vida.common.CurrentUser;
import com.vida.common.NotFoundException;
import com.vida.common.Strings;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Disciplinas e o que pertence a elas diretamente: horário e faltas. */
@Service
public class CourseService {

    static final String DEFAULT_COLOR = "#0f766e";

    private final CourseRepository courses;
    private final ClassSlotRepository slots;
    private final AbsenceRepository absences;
    private final AssessmentRepository assessments;
    private final TermService terms;
    private final CurrentUser currentUser;

    CourseService(CourseRepository courses, ClassSlotRepository slots, AbsenceRepository absences,
            AssessmentRepository assessments, TermService terms, CurrentUser currentUser) {
        this.courses = courses;
        this.slots = slots;
        this.absences = absences;
        this.assessments = assessments;
        this.terms = terms;
        this.currentUser = currentUser;
    }

    // ---------- Disciplinas ----------

    @Transactional(readOnly = true)
    public List<CourseResponse> list(UUID termId) {
        Term term = terms.find(termId);
        return toResponses(term, courses.findByUserIdAndTermIdOrderByName(currentUser.id(), termId));
    }

    @Transactional(readOnly = true)
    public CourseResponse get(UUID id) {
        Course course = find(id);
        return toResponses(terms.find(course.getTermId()), List.of(course)).getFirst();
    }

    @Transactional
    public CourseResponse create(CourseRequest req) {
        Term term = terms.find(req.termId());
        if (courses.existsByTermIdAndNameIgnoreCase(term.getId(), req.name().strip())) {
            throw BusinessException.conflict("Já existe uma disciplina com esse nome no semestre");
        }
        Course course = new Course(currentUser.id(), term.getId());
        apply(course, term, req);
        return toResponses(term, List.of(courses.save(course))).getFirst();
    }

    @Transactional
    public CourseResponse update(UUID id, CourseRequest req) {
        Course course = find(id);
        Term term = terms.find(req.termId());
        if (courses.existsByTermIdAndNameIgnoreCaseAndIdNot(term.getId(), req.name().strip(), id)) {
            throw BusinessException.conflict("Já existe uma disciplina com esse nome no semestre");
        }
        apply(course, term, req);
        return toResponses(term, List.of(course)).getFirst();
    }

    @Transactional
    public void delete(UUID id) {
        courses.delete(find(id));
    }

    Course find(UUID id) {
        return courses.findByIdAndUserId(id, currentUser.id())
                .orElseThrow(() -> new NotFoundException("Disciplina"));
    }

    Map<UUID, Course> findAll(Collection<UUID> ids) {
        return courses.findByUserIdAndIdIn(currentUser.id(), ids).stream()
                .collect(Collectors.toMap(Course::getId, Function.identity()));
    }

    private static void apply(Course course, Term term, CourseRequest req) {
        course.update(term.getId(), req.name().strip(), Strings.clean(req.code()), Strings.clean(req.professor()),
                req.workloadHours(), req.maxAbsences(),
                req.color() == null ? DEFAULT_COLOR : req.color().toLowerCase(),
                req.status() == null ? Course.Status.CURSANDO : req.status());
    }

    /** Monta as respostas com média e faltas, buscando avaliações e faltas de todas de uma vez. */
    private List<CourseResponse> toResponses(Term term, List<Course> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        UUID userId = currentUser.id();
        List<UUID> ids = list.stream().map(Course::getId).toList();

        Map<UUID, List<Grades.Item>> itemsByCourse = assessments.findByUserIdAndCourseIdIn(userId, ids).stream()
                .collect(Collectors.groupingBy(Assessment::getCourseId,
                        Collectors.mapping(a -> new Grades.Item(a.getWeight(), a.getMaxScore(), a.getScore()),
                                Collectors.toList())));
        Map<UUID, Integer> absencesByCourse = new HashMap<>();
        for (Object[] row : absences.sumByCourse(userId, ids)) {
            absencesByCourse.put((UUID) row[0], ((Number) row[1]).intValue());
        }

        return list.stream().map(c -> new CourseResponse(
                c.getId(), c.getTermId(), c.getName(), c.getCode(), c.getProfessor(), c.getWorkloadHours(),
                c.getMaxAbsences(), c.getColor(), c.getStatus(),
                gradeSummary(itemsByCourse.getOrDefault(c.getId(), List.of()), term.getPassingGrade()),
                absenceSummary(absencesByCourse.getOrDefault(c.getId(), 0), c.getMaxAbsences())))
                .toList();
    }

    static GradeSummary gradeSummary(List<Grades.Item> items, BigDecimal passing) {
        Grades.Summary s = Grades.summarize(items, passing);
        GradeStatus status;
        if (s.average() == null) {
            status = GradeStatus.SEM_NOTAS;
        } else if (s.neededOnPending() == null) {
            status = s.average().compareTo(passing) >= 0 ? GradeStatus.APROVADO : GradeStatus.REPROVADO;
        } else if (s.neededOnPending().signum() <= 0) {
            status = GradeStatus.APROVADO;
        } else if (s.neededOnPending().compareTo(BigDecimal.TEN) > 0) {
            status = GradeStatus.REPROVADO;
        } else if (s.neededOnPending().compareTo(passing) > 0) {
            status = GradeStatus.EM_RISCO;
        } else {
            status = GradeStatus.EM_DIA;
        }
        return new GradeSummary(s.average(), passing, s.gradedWeight(), s.pendingWeight(), s.neededOnPending(),
                status);
    }

    /** Atenção a partir de 75% do limite; estourado quando passa do limite. */
    static AbsenceSummary absenceSummary(int used, @Nullable Integer max) {
        AbsenceStatus status;
        if (max == null) {
            status = AbsenceStatus.SEM_LIMITE;
        } else if (used > max) {
            status = AbsenceStatus.ESTOURADO;
        } else if (used * 4 >= max * 3 && used > 0) {
            status = AbsenceStatus.ATENCAO;
        } else {
            status = AbsenceStatus.OK;
        }
        return new AbsenceSummary(used, max, status);
    }

    // ---------- Horário ----------

    @Transactional(readOnly = true)
    public List<SlotResponse> slots(UUID courseId) {
        Course course = find(courseId);
        return slots.findByUserIdAndCourseIdOrderByWeekdayAscStartTimeAsc(currentUser.id(), courseId).stream()
                .map(s -> toResponse(s, course))
                .toList();
    }

    /** Grade semanal do semestre (o atual, se nenhum for informado; vazia se não houver). */
    @Transactional(readOnly = true)
    public List<SlotResponse> schedule(@Nullable UUID termId) {
        Term term = termId != null ? terms.find(termId) : terms.current().orElse(null);
        if (term == null) {
            return List.of();
        }
        Map<UUID, Course> byId = courses.findByUserIdAndTermIdOrderByName(currentUser.id(), term.getId()).stream()
                .filter(c -> c.getStatus() != Course.Status.TRANCADO)
                .collect(Collectors.toMap(Course::getId, Function.identity()));
        if (byId.isEmpty()) {
            return List.of();
        }
        return slots.findByUserIdAndCourseIdInOrderByWeekdayAscStartTimeAsc(currentUser.id(), byId.keySet()).stream()
                .map(s -> toResponse(s, byId.get(s.getCourseId())))
                .toList();
    }

    @Transactional
    public SlotResponse addSlot(UUID courseId, SlotRequest req) {
        Course course = find(courseId);
        if (!req.endTime().isAfter(req.startTime())) {
            throw BusinessException.badRequest("A aula precisa terminar depois de começar");
        }
        ClassSlot slot = slots.save(new ClassSlot(currentUser.id(), course.getId(), DayOfWeek.of(req.weekday()),
                req.startTime(), req.endTime(), Strings.clean(req.room())));
        return toResponse(slot, course);
    }

    @Transactional
    public void deleteSlot(UUID slotId) {
        slots.delete(slots.findByIdAndUserId(slotId, currentUser.id())
                .orElseThrow(() -> new NotFoundException("Horário")));
    }

    private static SlotResponse toResponse(ClassSlot s, Course c) {
        return new SlotResponse(s.getId(), c.getId(), c.getName(), c.getColor(), s.getWeekday().getValue(),
                s.getStartTime(), s.getEndTime(), s.getRoom());
    }

    // ---------- Faltas ----------

    @Transactional(readOnly = true)
    public List<AbsenceResponse> absences(UUID courseId) {
        find(courseId);
        return absences.findByUserIdAndCourseIdOrderByDateDesc(currentUser.id(), courseId).stream()
                .map(CourseService::toResponse)
                .toList();
    }

    @Transactional
    public AbsenceResponse addAbsence(UUID courseId, AbsenceRequest req) {
        Course course = find(courseId);
        if (req.date().isAfter(currentUser.today())) {
            throw BusinessException.badRequest("Não dá para registrar falta no futuro");
        }
        Absence absence = absences.save(new Absence(currentUser.id(), course.getId(), req.date(),
                req.classesMissed() == null ? 1 : req.classesMissed(), Strings.clean(req.note())));
        return toResponse(absence);
    }

    @Transactional
    public void deleteAbsence(UUID absenceId) {
        absences.delete(absences.findByIdAndUserId(absenceId, currentUser.id())
                .orElseThrow(() -> new NotFoundException("Falta")));
    }

    private static AbsenceResponse toResponse(Absence a) {
        return new AbsenceResponse(a.getId(), a.getCourseId(), a.getDate(), a.getClassesMissed(), a.getNote());
    }
}
