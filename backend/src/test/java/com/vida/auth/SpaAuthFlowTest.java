package com.vida.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.vida.TestcontainersConfig;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Fluxo exatamente como o navegador faz, por HTTP de verdade: pega o cookie XSRF-TOKEN,
 * devolve no header X-XSRF-TOKEN e segue logado pelo cookie de sessão.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
class SpaAuthFlowTest {

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbc;

    private final CookieManager cookies = new CookieManager();
    private final HttpClient http = HttpClient.newBuilder().cookieHandler(cookies).build();

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("truncate table users cascade");
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + "/api" + path);
    }

    private HttpResponse<String> get(String path) throws Exception {
        return http.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String json, String xsrf) throws Exception {
        HttpRequest.Builder req = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));
        if (xsrf != null) {
            req.header("X-XSRF-TOKEN", xsrf);
        }
        return http.send(req.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String xsrfCookie() {
        return cookies.getCookieStore().getCookies().stream()
                .filter(c -> c.getName().equals("XSRF-TOKEN"))
                .map(HttpCookie::getValue)
                .findFirst().orElse(null);
    }

    @Test
    void cadastroLoginELogoutComoONavegador() throws Exception {
        HttpResponse<String> csrf = get("/auth/csrf");
        assertThat(csrf.statusCode()).isEqualTo(204);
        // path "/" para o JS das páginas (fora de /api) conseguir ler o cookie
        assertThat(csrf.headers().allValues("Set-Cookie"))
                .anySatisfy(c -> assertThat(c).startsWith("XSRF-TOKEN=").contains("Path=/;"));
        String xsrf = xsrfCookie();
        assertThat(xsrf).isNotBlank();

        String register = """
                {"name": "Eu", "email": "eu@exemplo.com", "password": "senha-forte-123"}""";
        assertThat(post("/auth/register", register, null).statusCode()).as("sem header CSRF").isEqualTo(403);
        assertThat(post("/auth/register", register, xsrf).statusCode()).isEqualTo(201);

        HttpResponse<String> me = get("/auth/me");
        assertThat(me.statusCode()).isEqualTo(200);
        assertThat(me.body()).contains("eu@exemplo.com");

        assertThat(post("/auth/logout", "", xsrfCookie()).statusCode()).isEqualTo(204);
        assertThat(get("/auth/me").statusCode()).isEqualTo(401);

        assertThat(post("/auth/login", """
                {"email": "EU@exemplo.com", "password": "senha-forte-123"}""", xsrfCookie()).statusCode())
                .isEqualTo(200);
        assertThat(get("/auth/me").statusCode()).isEqualTo(200);
    }
}
