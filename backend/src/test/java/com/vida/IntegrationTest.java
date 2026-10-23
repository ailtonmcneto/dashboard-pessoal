package com.vida;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vida.auth.User;
import com.vida.auth.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/** Base dos testes de API: contexto completo, Postgres real e banco limpo a cada teste. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
public abstract class IntegrationTest {

    protected static final String PASSWORD = "senha-forte-123";
    protected static final String SESSION_COOKIE = "SESSION";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("truncate table users, spring_session cascade");
    }

    /**
     * Cria o usuário direto no banco, loga pela API e devolve o cookie SESSION — o mesmo que o
     * navegador guardaria (a sessão em si fica no Postgres, via Spring Session JDBC).
     */
    protected Cookie loginAs(String email) throws Exception {
        users.save(new User(email, "Teste", passwordEncoder.encode(PASSWORD), "America/Sao_Paulo"));
        Cookie session = mvc.perform(post("/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}""".formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie(SESSION_COOKIE);
        assertThat(session).as("cookie de sessão").isNotNull();
        return session;
    }

    protected int storedSessions() {
        return jdbc.queryForObject("select count(*) from spring_session", Integer.class);
    }
}
