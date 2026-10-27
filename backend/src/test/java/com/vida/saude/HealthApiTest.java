package com.vida.saude;

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
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class HealthApiTest extends IntegrationTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");
    private final LocalDate today = LocalDate.now(SP);
    private Cookie me;

    @BeforeEach
    void login() throws Exception {
        me = loginAs("eu@exemplo.com");
    }

    private String metricId(String name) throws Exception {
        String json = getJson(me, "/metrics").andReturn().getResponse().getContentAsString();
        List<String> ids = JsonPath.read(json, "$[?(@.name == '%s')].id".formatted(name));
        return ids.getFirst();
    }

    /** Instante ao meio-dia (São Paulo) de um dia — longe da virada do dia. */
    private String noon(LocalDate day) {
        return day.atTime(12, 0).atZone(SP).toInstant().toString();
    }

    @Test
    void metricasSugeridasComValorDoDiaEMeta() throws Exception {
        postJson(me, "/metrics/defaults", "").andExpect(status().isCreated()).andExpect(jsonPath("$", hasSize(5)));
        String agua = metricId("Água");

        postJson(me, "/metrics/" + agua + "/measurements", """
                {"value": 1500, "measuredAt": "%s"}""".formatted(noon(today))).andExpect(status().isCreated());
        getJson(me, "/metrics")
                .andExpect(jsonPath("$[?(@.name == 'Água')].today").value(1500.0))
                .andExpect(jsonPath("$[?(@.name == 'Água')].todayStatus").value("NAO_ATINGIDA"));

        postJson(me, "/metrics/" + agua + "/measurements", """
                {"value": 600, "measuredAt": "%s"}""".formatted(noon(today))).andExpect(status().isCreated());
        postJson(me, "/metrics/" + agua + "/measurements", """
                {"value": 1000, "measuredAt": "%s"}""".formatted(noon(today.minusDays(2))))
                .andExpect(status().isCreated());
        getJson(me, "/metrics")
                .andExpect(jsonPath("$[?(@.name == 'Água')].today").value(2100.0))
                .andExpect(jsonPath("$[?(@.name == 'Água')].todayStatus").value("ATINGIDA"))
                .andExpect(jsonPath("$[?(@.name == 'Água')].average7").value(1550.0))
                .andExpect(jsonPath("$[?(@.name == 'Peso')].todayStatus").value("SEM_DADOS"));

        getJson(me, "/metrics/" + agua + "/series?from=%s&to=%s".formatted(today.minusDays(2), today))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].value").value(1000.0))
                .andExpect(jsonPath("$[0].met").value(false))
                .andExpect(jsonPath("$[1].value").doesNotExist())
                .andExpect(jsonPath("$[2].met").value(true));
        getJson(me, "/metrics/" + agua + "/measurements").andExpect(jsonPath("$", hasSize(3)));

        // peso: vale o último registro do dia
        String peso = metricId("Peso");
        postJson(me, "/metrics/" + peso + "/measurements", """
                {"value": 81.2, "measuredAt": "%s"}""".formatted(today.atTime(7, 0).atZone(SP).toInstant()))
                .andExpect(status().isCreated());
        postJson(me, "/metrics/" + peso + "/measurements", """
                {"value": 80.6, "measuredAt": "%s"}""".formatted(noon(today))).andExpect(status().isCreated());
        getJson(me, "/metrics")
                .andExpect(jsonPath("$[?(@.name == 'Peso')].today").value(80.6))
                .andExpect(jsonPath("$[?(@.name == 'Peso')].todayStatus").value("SEM_META"));
    }

    @Test
    void regrasDasMetricas() throws Exception {
        postJson(me, "/metrics", """
                {"name": "Tela", "unit": "min", "target": 120}""").andExpect(status().isBadRequest());
        String tela = create(me, "/metrics", """
                {"name": "Tela", "unit": "min", "target": 120, "targetKind": "MAXIMO"}""");
        postJson(me, "/metrics", "{\"name\": \"tela\", \"unit\": \"h\"}").andExpect(status().isConflict());
        postJson(me, "/metrics/" + tela + "/measurements", """
                {"value": 10, "measuredAt": "%s"}""".formatted(noon(today.plusDays(2))))
                .andExpect(status().isBadRequest());
        postJson(me, "/metrics/" + tela + "/measurements", "{\"value\": -1}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.value").exists());
        postJson(me, "/metrics/" + tela + "/measurements", "{\"value\": 90}")
                .andExpect(status().isCreated());
        getJson(me, "/metrics").andExpect(jsonPath("$[0].todayStatus").value("ATINGIDA"));

        putJson(me, "/metrics/" + tela, """
                {"name": "Tela", "unit": "min", "archived": true}""").andExpect(status().isOk());
        postJson(me, "/metrics/" + tela + "/measurements", "{\"value\": 5}").andExpect(status().isBadRequest());
        postJson(me, "/metrics/defaults", "").andExpect(status().isConflict());
    }

    @Test
    void habitoDiarioComSequencia() throws Exception {
        String id = create(me, "/habits", """
                {"name": "Ler 20 min", "startsOn": "%s"}""".formatted(today.minusDays(10)));
        for (int d = 1; d <= 3; d++) {
            putJson(me, "/habits/%s/checks/%s".formatted(id, today.minusDays(d)), "").andExpect(status().isOk());
        }
        getJson(me, "/habits")
                .andExpect(jsonPath("$[0].currentStreak").value(3))
                .andExpect(jsonPath("$[0].streakUnit").value("DIAS"))
                .andExpect(jsonPath("$[0].dueToday").value(true))
                .andExpect(jsonPath("$[0].doneToday").value(false))
                .andExpect(jsonPath("$[0].lastDays", hasSize(7)))
                .andExpect(jsonPath("$[0].lastDays[5].done").value(true))
                .andExpect(jsonPath("$[0].lastDays[6].done").value(false));

        // marcar é idempotente; hoje soma na sequência
        putJson(me, "/habits/%s/checks/%s".formatted(id, today), "").andExpect(jsonPath("$.currentStreak").value(4));
        putJson(me, "/habits/%s/checks/%s".formatted(id, today), "").andExpect(jsonPath("$.currentStreak").value(4));
        deleteJson(me, "/habits/%s/checks/%s".formatted(id, today.minusDays(2)))
                .andExpect(jsonPath("$.currentStreak").value(2))
                .andExpect(jsonPath("$.bestStreak").value(2));

        getJson(me, "/habits/%s/history?from=%s&to=%s".formatted(id, today.minusDays(7), today))
                .andExpect(jsonPath("$", hasSize(3)));

        putJson(me, "/habits/%s/checks/%s".formatted(id, today.plusDays(1)), "").andExpect(status().isBadRequest());
        putJson(me, "/habits/%s/checks/%s".formatted(id, today.minusDays(11)), "").andExpect(status().isBadRequest());
    }

    @Test
    void frequenciasValidam() throws Exception {
        postJson(me, "/habits", """
                {"name": "Academia", "schedule": "DIAS_SEMANA"}""").andExpect(status().isBadRequest());
        postJson(me, "/habits", """
                {"name": "Academia", "schedule": "DIAS_SEMANA", "weekdays": [1, 3, 8]}""")
                .andExpect(status().isBadRequest());
        postJson(me, "/habits", """
                {"name": "Academia", "schedule": "DIAS_SEMANA", "weekdays": [5, 1, 3]}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.weekdays", contains(1, 3, 5)));
        postJson(me, "/habits", """
                {"name": "Correr", "schedule": "X_POR_SEMANA"}""").andExpect(status().isBadRequest());
        postJson(me, "/habits", """
                {"name": "Correr", "schedule": "X_POR_SEMANA", "timesPerWeek": 3}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.streakUnit").value("SEMANAS"))
                .andExpect(jsonPath("$.targetThisWeek").value(3));
        postJson(me, "/habits", "{\"name\": \"correr\"}").andExpect(status().isConflict());
    }

    @Test
    void outroUsuarioNaoMexeNoQueEhMeu() throws Exception {
        postJson(me, "/metrics/defaults", "").andExpect(status().isCreated());
        String agua = metricId("Água");
        String habit = create(me, "/habits", "{\"name\": \"Ler\"}");
        Cookie other = loginAs("outro@exemplo.com");

        getJson(other, "/metrics").andExpect(jsonPath("$", hasSize(0)));
        getJson(other, "/habits").andExpect(jsonPath("$", hasSize(0)));
        postJson(other, "/metrics/" + agua + "/measurements", "{\"value\": 1}").andExpect(status().isNotFound());
        getJson(other, "/metrics/" + agua + "/series").andExpect(status().isNotFound());
        putJson(other, "/habits/%s/checks/%s".formatted(habit, today), "").andExpect(status().isNotFound());

        UUID otherUser = jdbc.queryForObject("select id from users where email = 'outro@exemplo.com'", UUID.class);
        assertThatThrownBy(() -> jdbc.update(
                "insert into habit_checks (user_id, habit_id, date) values (?, ?::uuid, current_date)", otherUser, habit))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
