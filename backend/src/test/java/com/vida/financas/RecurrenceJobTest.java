package com.vida.financas;

import static org.assertj.core.api.Assertions.assertThat;

import com.vida.IntegrationTest;
import com.vida.auth.UserRepository;
import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/** O job fica desligado nos testes; aqui ele é montado na mão e chamado direto. */
class RecurrenceJobTest extends IntegrationTest {

    @Autowired
    RecurrenceRepository recurrences;

    @Autowired
    RecurrenceService service;

    @Autowired
    UserRepository users;

    @Autowired
    TransactionTemplate tx;

    @Test
    void geraOcorrenciaVencidaNoFusoDoUsuarioSemRepetir() throws Exception {
        Cookie me = loginAs("eu@exemplo.com");
        LocalDate today = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        String account = create(me, "/accounts", "{\"name\": \"Corrente\"}");
        create(me, "/recurrences", """
                {"description": "Academia", "amount": 99.9, "kind": "DESPESA", "accountId": "%s",
                 "startsOn": "%s"}""".formatted(account, today));
        assertThat(count()).isEqualTo(1);

        // "passou um mês": a próxima ocorrência (índice 1) agora cai hoje
        jdbc.update("update recurrences set starts_on = ?, next_on = ?", today.minusMonths(1), today);

        RecurrenceJob job = new RecurrenceJob(recurrences, service, users, tx);
        Integer first = tx.execute(s -> job.generateAll());
        Integer second = tx.execute(s -> job.generateAll());
        assertThat(first).isEqualTo(1);
        assertThat(second).isZero();
        assertThat(count()).isEqualTo(2);
        assertThat(jdbc.queryForObject("select next_on from recurrences", LocalDate.class))
                .isEqualTo(today.minusMonths(1).plusMonths(2));
    }

    private int count() {
        return jdbc.queryForObject("select count(*) from transactions", Integer.class);
    }
}
