package com.vida.hoje;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.vida.IntegrationTest;
import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class HojeApiTest extends IntegrationTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");
    private final LocalDate today = LocalDate.now(SP);

    @Test
    void semDadosVemTudoVazio() throws Exception {
        Cookie me = loginAs("eu@exemplo.com");
        getJson(me, "/hoje")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value(today.toString()))
                .andExpect(jsonPath("$.alerts", hasSize(0)))
                .andExpect(jsonPath("$.tasks", hasSize(0)))
                .andExpect(jsonPath("$.money.totalBalance").value(0))
                .andExpect(jsonPath("$.studyMinutesWeek").value(0));
    }

    @Test
    void juntaOsModulosEOrdenaAlertas() throws Exception {
        Cookie me = loginAs("eu@exemplo.com");

        // tarefas
        create(me, "/tasks", "{\"title\": \"Atrasada\", \"dueOn\": \"%s\"}".formatted(today.minusDays(2)));
        create(me, "/tasks", "{\"title\": \"De hoje\", \"dueOn\": \"%s\"}".formatted(today));
        create(me, "/tasks", "{\"title\": \"Da semana que vem\", \"dueOn\": \"%s\"}".formatted(today.plusDays(8)));

        // estudos: aula hoje, prova hoje, nota em risco, faltas perto do limite, revisão
        String term = create(me, "/terms", """
                {"name": "2026.2", "startsOn": "%s", "endsOn": "%s"}""".formatted(today.minusDays(60), today.plusDays(60)));
        String calc = create(me, "/courses", """
                {"termId": "%s", "name": "Cálculo I", "maxAbsences": 8}""".formatted(term));
        postJson(me, "/courses/" + calc + "/slots", """
                {"weekday": %d, "startTime": "08:00", "endTime": "10:00"}""".formatted(today.getDayOfWeek().getValue()))
                .andExpect(status().isCreated());
        create(me, "/courses/" + calc + "/assessments", "{\"title\": \"P1\", \"score\": 4}");
        create(me, "/courses/" + calc + "/assessments", "{\"title\": \"P2\", \"dueOn\": \"%s\"}".formatted(today));
        create(me, "/courses/" + calc + "/absences",
                "{\"date\": \"%s\", \"classesMissed\": 6}".formatted(today.minusDays(1)));
        create(me, "/topics", "{\"title\": \"Limites\", \"courseId\": \"%s\"}".formatted(calc));
        create(me, "/study-sessions", """
                {"startedAt": "%s", "minutes": 30, "courseId": "%s"}"""
                .formatted(today.atTime(0, 30).atZone(SP).toInstant(), calc));

        // finanças: conta vencida e orçamento em 90%
        String conta = create(me, "/accounts", "{\"name\": \"Corrente\", \"openingBalance\": 1000}");
        postJson(me, "/categories/defaults", "").andExpect(status().isCreated());
        String json = getJson(me, "/categories").andReturn().getResponse().getContentAsString();
        String alimentacao = ((List<String>) JsonPath.read(json, "$[?(@.name == 'Alimentação')].id")).getFirst();
        putJson(me, "/budgets", """
                {"categoryId": "%s", "month": "%s", "limitAmount": 100}"""
                .formatted(alimentacao, today.toString().substring(0, 7))).andExpect(status().isOk());
        postJson(me, "/transactions", """
                {"kind": "DESPESA", "date": "%s", "description": "Mercado", "amount": 90, "accountId": "%s",
                 "categoryId": "%s"}""".formatted(today, conta, alimentacao)).andExpect(status().isCreated());
        postJson(me, "/transactions", """
                {"kind": "DESPESA", "date": "%s", "description": "Luz", "amount": 150, "accountId": "%s",
                 "paid": false}""".formatted(today.minusDays(3), conta)).andExpect(status().isCreated());

        // saúde, agenda e metas
        create(me, "/habits", "{\"name\": \"Ler\"}");
        create(me, "/metrics", "{\"name\": \"Água\", \"unit\": \"ml\", \"target\": 2000, \"targetKind\": \"MINIMO\"}");
        create(me, "/metrics", "{\"name\": \"Peso\", \"unit\": \"kg\"}");
        create(me, "/events", "{\"title\": \"Dentista\", \"startsAt\": \"%s\"}"
                .formatted(today.plusDays(1).atTime(14, 0).atZone(SP).toInstant()));
        create(me, "/goals", """
                {"title": "TCC", "startsOn": "%s", "dueOn": "%s"}""".formatted(today.minusDays(50), today.plusDays(50)));

        getJson(me, "/hoje")
                .andExpect(jsonPath("$.tasks[*].title", containsInAnyOrder("Atrasada", "De hoje")))
                .andExpect(jsonPath("$.classes", hasSize(1)))
                .andExpect(jsonPath("$.assessments[*].title", containsInAnyOrder("P2")))
                .andExpect(jsonPath("$.events[0].title").value("Dentista"))
                .andExpect(jsonPath("$.reviewsDue").value(1))
                .andExpect(jsonPath("$.habits[*].name", containsInAnyOrder("Ler")))
                .andExpect(jsonPath("$.metrics[*].name", containsInAnyOrder("Água")))
                .andExpect(jsonPath("$.money.totalBalance").value(910.0))
                .andExpect(jsonPath("$.money.pendingCount").value(1))
                .andExpect(jsonPath("$.studyMinutesWeek").value(30))
                // críticos primeiro
                .andExpect(jsonPath("$.alerts[0].severity").value("CRITICA"))
                .andExpect(jsonPath("$.alerts[*].kind", containsInAnyOrder(
                        "CONTA_VENCIDA", "PROVA_PROXIMA", "FALTAS", "NOTA_EM_RISCO", "TAREFA_ATRASADA",
                        "ORCAMENTO", "META_ATRASADA")))
                .andExpect(jsonPath("$.alerts[?(@.kind == 'CONTA_VENCIDA')].severity").value("CRITICA"))
                .andExpect(jsonPath("$.alerts[?(@.kind == 'PROVA_PROXIMA')].title").value("Hoje: P2 de Cálculo I"))
                .andExpect(jsonPath("$.alerts[?(@.kind == 'NOTA_EM_RISCO')].detail")
                        .value("Precisa de 8,0 no que falta"))
                .andExpect(jsonPath("$.alerts[?(@.kind == 'ORCAMENTO')].title")
                        .value("Orçamento de Alimentação em 90%"))
                .andExpect(jsonPath("$.alerts[*].link", hasItem("/estudos/disciplinas/" + calc)));
    }

    @Test
    void naoMisturaUsuarios() throws Exception {
        Cookie me = loginAs("eu@exemplo.com");
        create(me, "/tasks", "{\"title\": \"Minha\", \"dueOn\": \"%s\"}".formatted(today));
        Cookie other = loginAs("outro@exemplo.com");
        getJson(other, "/hoje").andExpect(jsonPath("$.tasks", hasSize(0)));
    }
}
