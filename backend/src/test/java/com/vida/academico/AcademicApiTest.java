package com.vida.academico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vida.IntegrationTest;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class AcademicApiTest extends IntegrationTest {

    private final LocalDate today = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
    private Cookie me;

    @BeforeEach
    void login() throws Exception {
        me = loginAs("eu@exemplo.com");
    }

    private String term(Cookie s, String name) throws Exception {
        return create(s, "/terms", """
                {"name": "%s", "startsOn": "%s", "endsOn": "%s"}"""
                .formatted(name, today.minusDays(30), today.plusDays(90)));
    }

    private String course(Cookie s, String termId, String name) throws Exception {
        return create(s, "/courses", """
                {"termId": "%s", "name": "%s", "maxAbsences": 8, "color": "#AA3366"}""".formatted(termId, name));
    }

    @Test
    void semestreDisciplinaMediaEFaltas() throws Exception {
        String termId = term(me, "2026.2");
        getJson(me, "/terms").andExpect(jsonPath("$[0].current").value(true))
                .andExpect(jsonPath("$[0].passingGrade").value(6.0));

        String calc = course(me, termId, "Cálculo I");
        postJson(me, "/courses/" + calc + "/assessments", """
                {"title": "P1", "weight": 1, "score": 4, "dueOn": "%s"}""".formatted(today.minusDays(10)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.normalizedScore").value(4.0))
                .andExpect(jsonPath("$.kind").value("PROVA"));
        postJson(me, "/courses/" + calc + "/assessments", """
                {"title": "P2", "weight": 1, "dueOn": "%s"}""".formatted(today.plusDays(5)))
                .andExpect(status().isCreated());
        postJson(me, "/courses/" + calc + "/absences", """
                {"date": "%s", "classesMissed": 6}""".formatted(today.minusDays(3)))
                .andExpect(status().isCreated());

        getJson(me, "/courses/" + calc)
                .andExpect(jsonPath("$.color").value("#aa3366"))
                .andExpect(jsonPath("$.grades.average").value(4.0))
                .andExpect(jsonPath("$.grades.neededOnPending").value(8.0))
                .andExpect(jsonPath("$.grades.status").value("EM_RISCO"))
                .andExpect(jsonPath("$.absences.used").value(6))
                .andExpect(jsonPath("$.absences.status").value("ATENCAO"));

        getJson(me, "/terms/" + termId + "/courses").andExpect(jsonPath("$", hasSize(1)));
        getJson(me, "/assessments/upcoming?days=7")
                .andExpect(jsonPath("$[*].title", contains("P2")))
                .andExpect(jsonPath("$[0].courseName").value("Cálculo I"));
    }

    @Test
    void validacoesDeRegra() throws Exception {
        String termId = term(me, "2026.2");
        postJson(me, "/terms", """
                {"name": "2026.2", "startsOn": "2026-01-01", "endsOn": "2026-06-01"}""")
                .andExpect(status().isConflict());
        postJson(me, "/terms", """
                {"name": "x", "startsOn": "2026-06-01", "endsOn": "2026-01-01"}""")
                .andExpect(status().isBadRequest());

        String calc = course(me, termId, "Cálculo I");
        postJson(me, "/courses", """
                {"termId": "%s", "name": "cálculo i"}""".formatted(termId))
                .andExpect(status().isConflict());
        postJson(me, "/courses/" + calc + "/assessments", """
                {"title": "P1", "maxScore": 10, "score": 11}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("A nota não pode passar da nota máxima (10)"));
        postJson(me, "/courses/" + calc + "/slots", """
                {"weekday": 2, "startTime": "10:00", "endTime": "08:00"}""")
                .andExpect(status().isBadRequest());
        postJson(me, "/courses/" + calc + "/slots", """
                {"weekday": 8, "startTime": "08:00", "endTime": "10:00"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.weekday").exists());
        postJson(me, "/courses/" + calc + "/absences", """
                {"date": "%s"}""".formatted(today.plusDays(1)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void gradeSemanalDoSemestreAtual() throws Exception {
        String termId = term(me, "2026.2");
        String calc = course(me, termId, "Cálculo I");
        String fis = course(me, termId, "Física I");
        postJson(me, "/courses/" + fis + "/slots", """
                {"weekday": 1, "startTime": "10:00", "endTime": "12:00", "room": "B2"}""")
                .andExpect(status().isCreated());
        postJson(me, "/courses/" + calc + "/slots", """
                {"weekday": 1, "startTime": "08:00", "endTime": "10:00"}""")
                .andExpect(status().isCreated());
        postJson(me, "/courses/" + calc + "/slots", """
                {"weekday": 3, "startTime": "08:00", "endTime": "10:00"}""")
                .andExpect(status().isCreated());

        getJson(me, "/schedule")
                .andExpect(jsonPath("$[*].courseName", contains("Cálculo I", "Física I", "Cálculo I")))
                .andExpect(jsonPath("$[1].room").value("B2"));

        // trancada sai da grade
        putJson(me, "/courses/" + fis, """
                {"termId": "%s", "name": "Física I", "status": "TRANCADO"}""".formatted(termId))
                .andExpect(status().isOk());
        getJson(me, "/schedule").andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void assuntosRevisaoEProvas() throws Exception {
        String calc = course(me, term(me, "2026.2"), "Cálculo I");
        String limites = create(me, "/topics", """
                {"title": "Limites", "courseId": "%s"}""".formatted(calc));
        String derivadas = create(me, "/topics", """
                {"title": "Derivadas", "courseId": "%s", "parentId": "%s"}""".formatted(calc, limites));

        // nunca revisados: entram na lista do dia
        getJson(me, "/topics?due=true").andExpect(jsonPath("$", hasSize(2)));

        postJson(me, "/topics/" + limites + "/review", "{\"mastery\": 3}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mastery").value(3))
                .andExpect(jsonPath("$.lastReviewedOn").value(today.toString()))
                .andExpect(jsonPath("$.nextReviewOn").value(today.plusDays(7).toString()))
                .andExpect(jsonPath("$.due").value(false));
        getJson(me, "/topics?due=true").andExpect(jsonPath("$[*].title", contains("Derivadas")));

        // ciclo: Limites dentro de Derivadas (que já está dentro de Limites)
        putJson(me, "/topics/" + limites, """
                {"title": "Limites", "courseId": "%s", "parentId": "%s"}""".formatted(calc, derivadas))
                .andExpect(status().isBadRequest());

        String resource = create(me, "/resources", "{\"title\": \"Livro\"}");
        postJson(me, "/topics", """
                {"title": "x", "courseId": "%s", "resourceId": "%s"}""".formatted(calc, resource))
                .andExpect(status().isBadRequest());

        // o que cai na prova
        postJson(me, "/courses/" + calc + "/assessments", """
                {"title": "P1", "topicIds": ["%s", "%s"]}""".formatted(limites, derivadas))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.topicIds", containsInAnyOrder(limites, derivadas)));
    }

    @Test
    void cursosLivresEProgresso() throws Exception {
        String id = create(me, "/resources", """
                {"title": "Spring na prática", "kind": "CURSO", "totalUnits": 40, "doneUnits": 10,
                 "url": "https://exemplo.com/curso"}""");
        getJson(me, "/resources")
                .andExpect(jsonPath("$[0].progressPercent").value(25))
                .andExpect(jsonPath("$[0].startedOn").value(today.toString()));

        putJson(me, "/resources/" + id, """
                {"title": "Spring na prática", "totalUnits": 40, "doneUnits": 10, "status": "CONCLUIDO"}""")
                .andExpect(jsonPath("$.doneUnits").value(40))
                .andExpect(jsonPath("$.progressPercent").value(100))
                .andExpect(jsonPath("$.finishedOn").value(today.toString()));

        postJson(me, "/resources", "{\"title\": \"x\", \"totalUnits\": 5, \"doneUnits\": 6}")
                .andExpect(status().isBadRequest());
        postJson(me, "/resources", "{\"title\": \"x\", \"url\": \"javascript:alert(1)\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.url").exists());
    }

    @Test
    void sessoesDeEstudoEResumo() throws Exception {
        String calc = course(me, term(me, "2026.2"), "Cálculo I");
        String livro = create(me, "/resources", "{\"title\": \"Livro de Java\"}");
        String limites = create(me, "/topics", """
                {"title": "Limites", "courseId": "%s"}""".formatted(calc));
        Instant now = Instant.now().truncatedTo(ChronoUnit.MINUTES);

        // só o assunto: herda a disciplina dele
        postJson(me, "/study-sessions", """
                {"startedAt": "%s", "minutes": 50, "topicId": "%s"}""".formatted(now.minus(2, ChronoUnit.HOURS), limites))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courseId").value(calc))
                .andExpect(jsonPath("$.label").value("Cálculo I · Limites"));
        create(me, "/study-sessions", """
                {"startedAt": "%s", "minutes": 30, "courseId": "%s"}""".formatted(now.minus(1, ChronoUnit.DAYS), calc));
        create(me, "/study-sessions", """
                {"startedAt": "%s", "minutes": 25, "resourceId": "%s"}""".formatted(now.minus(1, ChronoUnit.HOURS), livro));
        postJson(me, "/study-sessions", """
                {"startedAt": "%s", "minutes": 25}""".formatted(now.plus(1, ChronoUnit.DAYS)))
                .andExpect(status().isBadRequest());

        // 7 dias inteiros no fuso do usuário: de 6 dias atrás até o fim de hoje
        ZoneId zone = ZoneId.of("America/Sao_Paulo");
        String from = today.minusDays(6).atStartOfDay(zone).toInstant().toString();
        String to = today.plusDays(1).atStartOfDay(zone).toInstant().toString();
        getJson(me, "/study-sessions/summary?from=" + from + "&to=" + to)
                .andExpect(jsonPath("$.totalMinutes").value(105))
                .andExpect(jsonPath("$.bySubject[0].label").value("Cálculo I"))
                .andExpect(jsonPath("$.bySubject[0].minutes").value(80))
                .andExpect(jsonPath("$.bySubject[0].color").value("#aa3366"))
                .andExpect(jsonPath("$.bySubject[1].label").value("Livro de Java"))
                .andExpect(jsonPath("$.byDay", hasSize(7)))
                .andExpect(jsonPath("$.byDay[6].date").value(today.toString()));

        // apagar a disciplina mantém as horas, agora sem vínculo
        deleteJson(me, "/courses/" + calc).andExpect(status().isNoContent());
        getJson(me, "/study-sessions/summary?from=" + from + "&to=" + to)
                .andExpect(jsonPath("$.totalMinutes").value(105))
                .andExpect(jsonPath("$.bySubject[*].label", containsInAnyOrder("Sem vínculo", "Livro de Java")));
    }

    @Test
    void apagarSemestreLevaDisciplinasJunto() throws Exception {
        String termId = term(me, "2026.2");
        String calc = course(me, termId, "Cálculo I");
        postJson(me, "/courses/" + calc + "/assessments", "{\"title\": \"P1\"}").andExpect(status().isCreated());

        deleteJson(me, "/terms/" + termId).andExpect(status().isNoContent());
        getJson(me, "/courses/" + calc).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("select count(*) from assessments", Integer.class)).isZero();
    }

    @Test
    void outroUsuarioNaoEnxergaNemUsaDadosAlheios() throws Exception {
        String termId = term(me, "2026.2");
        String calc = course(me, termId, "Cálculo I");
        String topic = create(me, "/topics", "{\"title\": \"Limites\"}");
        Cookie other = loginAs("outro@exemplo.com");

        getJson(other, "/terms").andExpect(jsonPath("$", hasSize(0)));
        getJson(other, "/terms/" + termId + "/courses").andExpect(status().isNotFound());
        getJson(other, "/courses/" + calc).andExpect(status().isNotFound());
        postJson(other, "/courses", """
                {"termId": "%s", "name": "Intrusa"}""".formatted(termId)).andExpect(status().isNotFound());
        postJson(other, "/courses/" + calc + "/slots", """
                {"weekday": 1, "startTime": "08:00", "endTime": "10:00"}""").andExpect(status().isNotFound());
        postJson(other, "/topics/" + topic + "/review", "{\"mastery\": 5}").andExpect(status().isNotFound());
        postJson(other, "/study-sessions", """
                {"startedAt": "%s", "minutes": 10, "courseId": "%s"}""".formatted(Instant.now(), calc))
                .andExpect(status().isNotFound());

        // a prova do outro usuário não pode citar meu assunto
        String otherCourse = course(other, term(other, "Outro"), "Química");
        postJson(other, "/courses/" + otherCourse + "/assessments", """
                {"title": "P1", "topicIds": ["%s"]}""".formatted(topic)).andExpect(status().isNotFound());
    }

    @Test
    void bancoRecusaVinculoEntreUsuariosDiferentes() throws Exception {
        String termId = term(me, "2026.2");
        loginAs("outro@exemplo.com");
        UUID otherUser = jdbc.queryForObject("select id from users where email = 'outro@exemplo.com'", UUID.class);

        // mesmo passando pela aplicação, a FK composta (term_id, user_id) barra
        assertThatThrownBy(() -> jdbc.update(
                "insert into courses (user_id, term_id, name) values (?, ?::uuid, 'Intrusa')", otherUser, termId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
