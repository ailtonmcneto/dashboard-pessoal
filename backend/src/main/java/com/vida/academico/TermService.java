package com.vida.academico;

import com.vida.academico.AcademicDtos.TermRequest;
import com.vida.academico.AcademicDtos.TermResponse;
import com.vida.common.BusinessException;
import com.vida.common.CurrentUser;
import com.vida.common.NotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TermService {

    static final BigDecimal DEFAULT_PASSING_GRADE = new BigDecimal("6.00");

    private final TermRepository terms;
    private final CurrentUser currentUser;

    TermService(TermRepository terms, CurrentUser currentUser) {
        this.terms = terms;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<TermResponse> list() {
        LocalDate today = currentUser.today();
        return terms.findByUserIdOrderByStartsOnDesc(currentUser.id()).stream()
                .map(t -> toResponse(t, today))
                .toList();
    }

    /** Semestre que contém hoje (o que começou por último, se houver sobreposição). */
    @Transactional(readOnly = true)
    public Optional<Term> current() {
        LocalDate today = currentUser.today();
        return terms.findByUserIdOrderByStartsOnDesc(currentUser.id()).stream()
                .filter(t -> t.contains(today))
                .findFirst();
    }

    @Transactional
    public TermResponse create(TermRequest req) {
        if (terms.existsByUserIdAndNameIgnoreCase(currentUser.id(), req.name().strip())) {
            throw BusinessException.conflict("Já existe um semestre com esse nome");
        }
        Term term = new Term(currentUser.id());
        apply(term, req);
        return toResponse(terms.save(term), currentUser.today());
    }

    @Transactional
    public TermResponse update(UUID id, TermRequest req) {
        Term term = find(id);
        if (terms.existsByUserIdAndNameIgnoreCaseAndIdNot(currentUser.id(), req.name().strip(), id)) {
            throw BusinessException.conflict("Já existe um semestre com esse nome");
        }
        apply(term, req);
        return toResponse(term, currentUser.today());
    }

    /** Apaga o semestre com disciplinas, horários, faltas e avaliações. */
    @Transactional
    public void delete(UUID id) {
        terms.delete(find(id));
    }

    Term find(UUID id) {
        return terms.findByIdAndUserId(id, currentUser.id()).orElseThrow(() -> new NotFoundException("Semestre"));
    }

    private static void apply(Term term, TermRequest req) {
        if (req.endsOn().isBefore(req.startsOn())) {
            throw BusinessException.badRequest("O fim do semestre não pode ser antes do início");
        }
        term.update(req.name().strip(), req.startsOn(), req.endsOn(),
                req.passingGrade() == null ? DEFAULT_PASSING_GRADE : req.passingGrade());
    }

    static TermResponse toResponse(Term t, LocalDate today) {
        return new TermResponse(t.getId(), t.getName(), t.getStartsOn(), t.getEndsOn(), t.getPassingGrade(),
                t.contains(today));
    }
}
