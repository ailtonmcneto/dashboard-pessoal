package com.vida.agenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.vida.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

class EventApiTest extends IntegrationTest {

    private static final String CONSULTA = """
            {"title": "Dentista", "kind": "CONSULTA", "startsAt": "2026-11-03T13:00:00Z",
             "endsAt": "2026-11-03T14:00:00Z", "location": "  Centro  ", "notes": " "}""";

    private static final String RANGE = "?from=2026-11-01T00:00:00Z&to=2026-12-01T00:00:00Z";

    private String create(MockHttpSession session, String body) throws Exception {
        String json = mvc.perform(post("/events").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    @Test
    void crudCompleto() throws Exception {
        MockHttpSession session = loginAs("eu@exemplo.com");
        String id = create(session, CONSULTA);
        assertThat(id).as("uuid v7").matches("[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-.*");

        mvc.perform(get("/events" + RANGE).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].kind").value("CONSULTA"))
                .andExpect(jsonPath("$[0].location").value("Centro"))
                .andExpect(jsonPath("$[0].notes").doesNotExist());

        mvc.perform(put("/events/" + id).session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Dentista (remarcado)", "startsAt": "2026-11-05T13:00:00Z"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Dentista (remarcado)"))
                .andExpect(jsonPath("$.kind").value("COMPROMISSO"));

        mvc.perform(delete("/events/" + id).session(session).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/events" + RANGE).session(session)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listaSoOIntervaloPedidoEmOrdem() throws Exception {
        MockHttpSession session = loginAs("eu@exemplo.com");
        create(session, """
                {"title": "B", "startsAt": "2026-11-20T10:00:00Z"}""");
        create(session, """
                {"title": "A", "startsAt": "2026-11-10T10:00:00Z"}""");
        create(session, """
                {"title": "Fora", "startsAt": "2026-12-01T00:00:00Z"}""");

        mvc.perform(get("/events" + RANGE).session(session))
                .andExpect(jsonPath("$[*].title").value(org.hamcrest.Matchers.contains("A", "B")));
    }

    @Test
    void validaDadosERegras() throws Exception {
        MockHttpSession session = loginAs("eu@exemplo.com");

        mvc.perform(post("/events").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\": \" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.startsAt").exists());

        mvc.perform(post("/events").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "x", "startsAt": "2026-11-03T13:00:00Z", "endsAt": "2026-11-03T12:00:00Z"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("O fim não pode ser antes do início"));

        mvc.perform(get("/events?from=2026-01-01T00:00:00Z&to=2028-01-01T00:00:00Z").session(session))
                .andExpect(status().isBadRequest());
    }

    @Test
    void usuarioNaoVeNemAlteraEventoDeOutro() throws Exception {
        MockHttpSession a = loginAs("a@exemplo.com");
        MockHttpSession b = loginAs("b@exemplo.com");
        String idDeA = create(a, CONSULTA);

        mvc.perform(get("/events" + RANGE).session(b)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(put("/events/" + idDeA).session(b).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(CONSULTA))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/events/" + idDeA).session(b).with(csrf())).andExpect(status().isNotFound());

        mvc.perform(get("/events" + RANGE).session(a)).andExpect(jsonPath("$[0].title").value("Dentista"));
    }
}
