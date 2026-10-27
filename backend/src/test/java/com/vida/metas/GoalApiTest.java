package com.vida.metas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.vida.IntegrationTest;
import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class GoalApiTest extends IntegrationTest {

    private final LocalDate today = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
    private Cookie me;

    @BeforeEach
    void login() throws Exception {
        me = loginAs("eu@exemplo.com");
    }

    private String milestoneId(String goalJson, int index) {
        return JsonPath.read(goalJson, "$.milestones[%d].id".formatted(index));
    }

    @Test
    void metaPorMarcosComRitmo() throws Exception {
        String id = create(me, "/goals", """
                {"title": "Terminar o TCC", "area": "ESTUDOS", "startsOn": "%s", "dueOn": "%s"}"""
                .formatted(today.minusDays(50), today.plusDays(50)));
        String json = "";
        for (String m : new String[] {"Tema", "Revisão bibliográfica", "Escrita", "Defesa"}) {
            json = postJson(me, "/goals/" + id + "/milestones", "{\"title\": \"%s\"}".formatted(m))
                    .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        }
        postJson(me, "/milestones/" + milestoneId(json, 0) + "/done", "").andExpect(status().isOk());
        postJson(me, "/milestones/" + milestoneId(json, 1) + "/done", "")
                .andExpect(jsonPath("$.progressMode").value("MARCOS"))
                .andExpect(jsonPath("$.current").value(2))
                .andExpect(jsonPath("$.target").value(4))
                .andExpect(jsonPath("$.percent").value(50))
                .andExpect(jsonPath("$.expectedPercent").value(50))
                .andExpect(jsonPath("$.pace").value("NO_RITMO"))
                .andExpect(jsonPath("$.milestones[*].title",
                        contains("Tema", "Revisão bibliográfica", "Escrita", "Defesa")))
                .andExpect(jsonPath("$.milestones[0].doneOn").value(today.toString()));

        postJson(me, "/milestones/" + milestoneId(json, 1) + "/undone", "")
                .andExpect(jsonPath("$.percent").value(25))
                .andExpect(jsonPath("$.pace").value("ATRASADA"));

        putJson(me, "/goals/" + id + "/status", "{\"status\": \"CONCLUIDA\"}")
                .andExpect(jsonPath("$.pace").value("CONCLUIDA"))
                .andExpect(jsonPath("$.completedOn").value(today.toString()));
        putJson(me, "/goals/" + id + "/status", "{\"status\": \"ATIVA\"}")
                .andExpect(jsonPath("$.completedOn").doesNotExist());
    }

    @Test
    void metaManualEFinanceira() throws Exception {
        postJson(me, "/goals", """
                {"title": "Ler livros", "progressMode": "MANUAL"}""").andExpect(status().isBadRequest());
        postJson(me, "/goals", """
                {"title": "Ler 12 livros", "progressMode": "MANUAL", "targetValue": 12, "currentValue": 3,
                 "unit": "livros"}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.percent").value(25))
                .andExpect(jsonPath("$.pace").value("SEM_PRAZO"));

        String conta = create(me, "/accounts", "{\"name\": \"Reserva viagem\", \"openingBalance\": 2500}");
        postJson(me, "/goals", """
                {"title": "Viagem", "progressMode": "FINANCEIRO", "targetValue": 10000}""")
                .andExpect(status().isBadRequest());
        postJson(me, "/goals", """
                {"title": "Viagem", "area": "FINANCAS", "progressMode": "FINANCEIRO", "targetValue": 10000,
                 "accountId": "%s"}""".formatted(conta))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.current").value(2500.0))
                .andExpect(jsonPath("$.percent").value(25))
                .andExpect(jsonPath("$.accountName").value("Reserva viagem"));

        // depósito na conta move o progresso
        postJson(me, "/transactions", """
                {"kind": "RECEITA", "date": "%s", "description": "Aporte", "amount": 2500, "accountId": "%s"}"""
                .formatted(today, conta)).andExpect(status().isCreated());
        getJson(me, "/goals").andExpect(jsonPath("$[?(@.title == 'Viagem')].percent").value(50));
    }

    @Test
    void tarefasComVinculosEVisoes() throws Exception {
        String goal = create(me, "/goals", "{\"title\": \"Correr 10 km\", \"progressMode\": \"TAREFAS\"}");
        String milestoneJson = postJson(me, "/goals/" + goal + "/milestones", "{\"title\": \"5 km\"}")
                .andReturn().getResponse().getContentAsString();
        String milestone = milestoneId(milestoneJson, 0);
        String project = create(me, "/projects", """
                {"title": "Portfólio", "kind": "ESTUDO", "goalId": "%s"}""".formatted(goal));

        // tarefa do marco vira tarefa da meta automaticamente
        postJson(me, "/tasks", """
                {"title": "Treino intervalado", "milestoneId": "%s", "dueOn": "%s", "priority": 1}"""
                .formatted(milestone, today.minusDays(1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.goalId").value(goal))
                .andExpect(jsonPath("$.goalTitle").value("Correr 10 km"))
                .andExpect(jsonPath("$.overdue").value(true));
        String hoje = create(me, "/tasks", """
                {"title": "Comprar tênis", "goalId": "%s", "dueOn": "%s"}""".formatted(goal, today));
        create(me, "/tasks", """
                {"title": "Deploy do site", "projectId": "%s", "dueOn": "%s"}""".formatted(project, today.plusDays(3)));
        create(me, "/tasks", "{\"title\": \"Sem prazo\", \"priority\": 3}");

        getJson(me, "/tasks?view=HOJE")
                .andExpect(jsonPath("$[*].title", contains("Treino intervalado", "Comprar tênis")));
        getJson(me, "/tasks?view=SEMANA").andExpect(jsonPath("$", hasSize(3)));
        getJson(me, "/tasks").andExpect(jsonPath("$[3].title").value("Sem prazo"));
        getJson(me, "/tasks?projectId=" + project)
                .andExpect(jsonPath("$[0].projectTitle").value("Portfólio"));

        postJson(me, "/tasks/" + hoje + "/done", "").andExpect(jsonPath("$.done").value(true));
        getJson(me, "/tasks?view=FEITAS").andExpect(jsonPath("$[*].title", contains("Comprar tênis")));
        getJson(me, "/goals/" + goal)
                .andExpect(jsonPath("$.percent").value(50))
                .andExpect(jsonPath("$.openTasks").value(1))
                .andExpect(jsonPath("$.doneTasks").value(1));
        getJson(me, "/projects")
                .andExpect(jsonPath("$[0].goalTitle").value("Correr 10 km"))
                .andExpect(jsonPath("$[0].openTasks").value(1))
                .andExpect(jsonPath("$[0].percent").value(0));

        // apagar a meta não apaga tarefas nem projeto
        deleteJson(me, "/goals/" + goal).andExpect(status().isNoContent());
        getJson(me, "/tasks").andExpect(jsonPath("$", hasSize(3)));
        getJson(me, "/projects").andExpect(jsonPath("$[0].goalId").doesNotExist());
    }

    @Test
    void tarefaDeDisciplina() throws Exception {
        String term = create(me, "/terms", """
                {"name": "2026.2", "startsOn": "%s", "endsOn": "%s"}""".formatted(today.minusDays(30), today.plusDays(60)));
        String course = create(me, "/courses", """
                {"termId": "%s", "name": "Cálculo I"}""".formatted(term));
        postJson(me, "/tasks", """
                {"title": "Lista 3", "courseId": "%s"}""".formatted(course))
                .andExpect(jsonPath("$.courseName").value("Cálculo I"));
        getJson(me, "/tasks?courseId=" + course).andExpect(jsonPath("$", hasSize(1)));

        // marco de uma meta não pode ir para tarefa de outra meta
        String g1 = create(me, "/goals", "{\"title\": \"A\"}");
        String g2 = create(me, "/goals", "{\"title\": \"B\"}");
        String m1 = milestoneId(postJson(me, "/goals/" + g1 + "/milestones", "{\"title\": \"m\"}")
                .andReturn().getResponse().getContentAsString(), 0);
        postJson(me, "/tasks", """
                {"title": "x", "goalId": "%s", "milestoneId": "%s"}""".formatted(g2, m1))
                .andExpect(status().isBadRequest());
    }

    @Test
    void outroUsuarioNaoMexeNoQueEhMeu() throws Exception {
        String goal = create(me, "/goals", "{\"title\": \"Minha meta\"}");
        String task = create(me, "/tasks", "{\"title\": \"Minha tarefa\"}");
        String conta = create(me, "/accounts", "{\"name\": \"Minha conta\"}");
        Cookie other = loginAs("outro@exemplo.com");

        getJson(other, "/goals").andExpect(jsonPath("$", hasSize(0)));
        getJson(other, "/tasks").andExpect(jsonPath("$", hasSize(0)));
        getJson(other, "/goals/" + goal).andExpect(status().isNotFound());
        postJson(other, "/tasks/" + task + "/done", "").andExpect(status().isNotFound());
        postJson(other, "/tasks", """
                {"title": "x", "goalId": "%s"}""".formatted(goal)).andExpect(status().isNotFound());
        postJson(other, "/goals", """
                {"title": "x", "progressMode": "FINANCEIRO", "targetValue": 1, "accountId": "%s"}"""
                .formatted(conta)).andExpect(status().isNotFound());

        UUID otherUser = jdbc.queryForObject("select id from users where email = 'outro@exemplo.com'", UUID.class);
        assertThatThrownBy(() -> jdbc.update(
                "insert into tasks (user_id, title, goal_id) values (?, 'x', ?::uuid)", otherUser, goal))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("select count(*) from tasks", Integer.class)).isEqualTo(1);
    }
}
