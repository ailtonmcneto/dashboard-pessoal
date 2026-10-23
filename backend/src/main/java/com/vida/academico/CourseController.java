package com.vida.academico;

import com.vida.academico.AcademicDtos.AbsenceRequest;
import com.vida.academico.AcademicDtos.AbsenceResponse;
import com.vida.academico.AcademicDtos.AssessmentRequest;
import com.vida.academico.AcademicDtos.AssessmentResponse;
import com.vida.academico.AcademicDtos.CourseRequest;
import com.vida.academico.AcademicDtos.CourseResponse;
import com.vida.academico.AcademicDtos.SlotRequest;
import com.vida.academico.AcademicDtos.SlotResponse;
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

/** Disciplinas e o que fica pendurado nelas: horário, faltas e avaliações. */
@RestController
class CourseController {

    private final CourseService courses;
    private final AssessmentService assessments;

    CourseController(CourseService courses, AssessmentService assessments) {
        this.courses = courses;
        this.assessments = assessments;
    }

    @PostMapping("/courses")
    @ResponseStatus(HttpStatus.CREATED)
    CourseResponse create(@Valid @RequestBody CourseRequest req) {
        return courses.create(req);
    }

    @GetMapping("/courses/{id}")
    CourseResponse get(@PathVariable UUID id) {
        return courses.get(id);
    }

    @PutMapping("/courses/{id}")
    CourseResponse update(@PathVariable UUID id, @Valid @RequestBody CourseRequest req) {
        return courses.update(id, req);
    }

    @DeleteMapping("/courses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        courses.delete(id);
    }

    // ---------- Horário ----------

    @GetMapping("/courses/{id}/slots")
    List<SlotResponse> slots(@PathVariable UUID id) {
        return courses.slots(id);
    }

    @PostMapping("/courses/{id}/slots")
    @ResponseStatus(HttpStatus.CREATED)
    SlotResponse addSlot(@PathVariable UUID id, @Valid @RequestBody SlotRequest req) {
        return courses.addSlot(id, req);
    }

    @DeleteMapping("/slots/{slotId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteSlot(@PathVariable UUID slotId) {
        courses.deleteSlot(slotId);
    }

    /** Grade semanal; sem termId, a do semestre atual. */
    @GetMapping("/schedule")
    List<SlotResponse> schedule(@RequestParam(required = false) @Nullable UUID termId) {
        return courses.schedule(termId);
    }

    // ---------- Faltas ----------

    @GetMapping("/courses/{id}/absences")
    List<AbsenceResponse> absences(@PathVariable UUID id) {
        return courses.absences(id);
    }

    @PostMapping("/courses/{id}/absences")
    @ResponseStatus(HttpStatus.CREATED)
    AbsenceResponse addAbsence(@PathVariable UUID id, @Valid @RequestBody AbsenceRequest req) {
        return courses.addAbsence(id, req);
    }

    @DeleteMapping("/absences/{absenceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteAbsence(@PathVariable UUID absenceId) {
        courses.deleteAbsence(absenceId);
    }

    // ---------- Avaliações ----------

    @GetMapping("/courses/{id}/assessments")
    List<AssessmentResponse> assessments(@PathVariable UUID id) {
        return assessments.list(id);
    }

    @PostMapping("/courses/{id}/assessments")
    @ResponseStatus(HttpStatus.CREATED)
    AssessmentResponse addAssessment(@PathVariable UUID id, @Valid @RequestBody AssessmentRequest req) {
        return assessments.create(id, req);
    }

    @PutMapping("/assessments/{assessmentId}")
    AssessmentResponse updateAssessment(@PathVariable UUID assessmentId, @Valid @RequestBody AssessmentRequest req) {
        return assessments.update(assessmentId, req);
    }

    @DeleteMapping("/assessments/{assessmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteAssessment(@PathVariable UUID assessmentId) {
        assessments.delete(assessmentId);
    }

    /** Próximas avaliações (de hoje a {@code days} dias) das disciplinas em curso. */
    @GetMapping("/assessments/upcoming")
    List<AssessmentResponse> upcoming(@RequestParam(defaultValue = "14") int days) {
        return assessments.upcoming(days);
    }
}
