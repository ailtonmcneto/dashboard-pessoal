package com.vida.academico;

import static org.assertj.core.api.Assertions.assertThat;

import com.vida.academico.AcademicDtos.AbsenceStatus;
import com.vida.academico.AcademicDtos.GradeStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class GradesTest {

    private static final BigDecimal SIX = new BigDecimal("6");

    private static Grades.Item item(String weight, String max, String score) {
        return new Grades.Item(new BigDecimal(weight), new BigDecimal(max), score == null ? null : new BigDecimal(score));
    }

    @Test
    void mediaPonderadaNormalizaNotasComMaximosDiferentes() {
        // 8/10 com peso 2, 15/20 (= 7,5) com peso 1 → (16 + 7,5) / 3 = 7,83
        Grades.Summary s = Grades.summarize(List.of(item("2", "10", "8"), item("1", "20", "15")), SIX);
        assertThat(s.average()).isEqualByComparingTo("7.83");
        assertThat(s.pendingWeight()).isEqualByComparingTo("0");
        assertThat(s.neededOnPending()).isNull();
    }

    @Test
    void calculaQuantoPrecisaNoQueFalta() {
        // P1 = 4 (peso 1), P2 pendente (peso 1): precisa de 8 na P2 para média 6
        Grades.Summary s = Grades.summarize(List.of(item("1", "10", "4"), item("1", "10", null)), SIX);
        assertThat(s.average()).isEqualByComparingTo("4");
        assertThat(s.neededOnPending()).isEqualByComparingTo("8");
    }

    @Test
    void semAvaliacoesNaoTemMedia() {
        Grades.Summary s = Grades.summarize(List.of(), SIX);
        assertThat(s.average()).isNull();
        assertThat(s.neededOnPending()).isNull();
    }

    @Test
    void situacaoDaNota() {
        assertThat(status(item("1", "10", null))).isEqualTo(GradeStatus.SEM_NOTAS);
        assertThat(status(item("1", "10", "5"), item("1", "10", null))).isEqualTo(GradeStatus.EM_RISCO); // precisa 7
        assertThat(status(item("1", "10", "7"), item("1", "10", null))).isEqualTo(GradeStatus.EM_DIA); // precisa 5
        assertThat(status(item("3", "10", "10"), item("1", "10", null))).isEqualTo(GradeStatus.APROVADO); // precisa < 0
        assertThat(status(item("3", "10", "1"), item("1", "10", null))).isEqualTo(GradeStatus.REPROVADO); // precisa 21
        assertThat(status(item("1", "10", "6"))).isEqualTo(GradeStatus.APROVADO);
        assertThat(status(item("1", "10", "5.99"))).isEqualTo(GradeStatus.REPROVADO);
    }

    private static GradeStatus status(Grades.Item... items) {
        return CourseService.gradeSummary(List.of(items), SIX).status();
    }

    @Test
    void situacaoDasFaltas() {
        assertThat(CourseService.absenceSummary(3, null).status()).isEqualTo(AbsenceStatus.SEM_LIMITE);
        assertThat(CourseService.absenceSummary(0, 8).status()).isEqualTo(AbsenceStatus.OK);
        assertThat(CourseService.absenceSummary(5, 8).status()).isEqualTo(AbsenceStatus.OK);
        assertThat(CourseService.absenceSummary(6, 8).status()).isEqualTo(AbsenceStatus.ATENCAO); // 75%
        assertThat(CourseService.absenceSummary(8, 8).status()).isEqualTo(AbsenceStatus.ATENCAO);
        assertThat(CourseService.absenceSummary(9, 8).status()).isEqualTo(AbsenceStatus.ESTOURADO);
    }

    @Test
    void revisaoEspacadaAfastaConformeODominio() {
        LocalDate d = LocalDate.of(2026, 10, 23);
        assertThat(Reviews.nextReview(0, d)).isEqualTo(d.plusDays(1));
        assertThat(Reviews.nextReview(3, d)).isEqualTo(d.plusDays(7));
        assertThat(Reviews.nextReview(5, d)).isEqualTo(d.plusDays(30));
    }
}
