package com.vida.academico;

import com.vida.academico.AcademicDtos.CourseResponse;
import com.vida.academico.AcademicDtos.TermRequest;
import com.vida.academico.AcademicDtos.TermResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/terms")
class TermController {

    private final TermService terms;
    private final CourseService courses;

    TermController(TermService terms, CourseService courses) {
        this.terms = terms;
        this.courses = courses;
    }

    @GetMapping
    List<TermResponse> list() {
        return terms.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TermResponse create(@Valid @RequestBody TermRequest req) {
        return terms.create(req);
    }

    @PutMapping("/{id}")
    TermResponse update(@PathVariable UUID id, @Valid @RequestBody TermRequest req) {
        return terms.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        terms.delete(id);
    }

    /** Disciplinas do semestre, já com média e faltas. */
    @GetMapping("/{id}/courses")
    List<CourseResponse> courses(@PathVariable UUID id) {
        return courses.list(id);
    }
}
