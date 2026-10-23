package com.vida.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vida.IntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AuthApiTest extends IntegrationTest {

    private static final String REGISTER = """
            {"name": "Ailton", "email": "Eu@Exemplo.com", "password": "%s"}""".formatted(PASSWORD);

    @Test
    void primeiroCadastroLogaENormalizaEmail() throws Exception {
        mvc.perform(get("/auth/status")).andExpect(jsonPath("$.signupOpen").value(true));

        Cookie session = mvc.perform(post("/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(REGISTER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("eu@exemplo.com"))
                .andExpect(jsonPath("$.timezone").value("America/Sao_Paulo"))
                .andReturn().getResponse().getCookie(SESSION_COOKIE);

        mvc.perform(get("/auth/me").cookie(session))
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
        Cookie session = loginAs("eu@exemplo.com");

        mvc.perform(post("/events").cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "x", "startsAt": "2026-11-01T12:00:00Z"}"""))
                .andExpect(status().isForbidden());
    }


    @Test
    void sessaoFicaNoBancoELogoutApaga() throws Exception {
        Cookie session = loginAs("eu@exemplo.com");
        assertThat(storedSessions()).isEqualTo(1);
        assertThat(jdbc.queryForObject("select principal_name from spring_session", String.class))
                .isEqualTo("eu@exemplo.com");

        mvc.perform(post("/auth/logout").cookie(session).with(csrf())).andExpect(status().isNoContent());
        assertThat(storedSessions()).isZero();
        mvc.perform(get("/auth/me").cookie(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void sessaoNaoDependeDaMemoriaDaApi() throws Exception {
        Cookie session = loginAs("eu@exemplo.com");

        // Simula outra instância/um restart: nada em memória, só o cookie e a linha no banco.
        Cookie copia = new Cookie(SESSION_COOKIE, session.getValue());
        mvc.perform(get("/auth/me").cookie(copia)).andExpect(status().isOk());

        jdbc.update("delete from spring_session");
        mvc.perform(get("/auth/me").cookie(copia)).andExpect(status().isUnauthorized());
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
