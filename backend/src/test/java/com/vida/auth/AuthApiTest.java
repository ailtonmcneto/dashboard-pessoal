package com.vida.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vida.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

class AuthApiTest extends IntegrationTest {

    private static final String REGISTER = """
            {"name": "Ailton", "email": "Eu@Exemplo.com", "password": "%s"}""".formatted(PASSWORD);

    @Test
    void primeiroCadastroLogaENormalizaEmail() throws Exception {
        mvc.perform(get("/auth/status")).andExpect(jsonPath("$.signupOpen").value(true));

        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/auth/register").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(REGISTER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("eu@exemplo.com"))
                .andExpect(jsonPath("$.timezone").value("America/Sao_Paulo"));

        mvc.perform(get("/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ailton"));
        mvc.perform(get("/auth/status")).andExpect(jsonPath("$.signupOpen").value(false));
    }

    @Test
    void segundoCadastroEhRecusado() throws Exception {
        loginAs("dono@exemplo.com");

        mvc.perform(post("/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(REGISTER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Cadastro fechado"));
    }

    @Test
    void senhaErradaDa401() throws Exception {
        loginAs("eu@exemplo.com");

        mvc.perform(post("/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "eu@exemplo.com", "password": "errada-123"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos"));
    }

    @Test
    void rotasProtegidasExigemLogin() throws Exception {
        mvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/events")).andExpect(status().isUnauthorized());
    }

    @Test
    void postSemCsrfEhRecusado() throws Exception {
        MockHttpSession session = loginAs("eu@exemplo.com");

        mvc.perform(post("/events").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "x", "startsAt": "2026-11-01T12:00:00Z"}"""))
                .andExpect(status().isForbidden());
    }


    @Test
    void logoutEncerraASessao() throws Exception {
        MockHttpSession session = loginAs("eu@exemplo.com");

        mvc.perform(post("/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/auth/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void cadastroValidaCampos() throws Exception {
        mvc.perform(post("/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "", "email": "nao-e-email", "password": "curta"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }
}
