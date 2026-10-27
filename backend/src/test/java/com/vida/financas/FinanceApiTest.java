package com.vida.financas;

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
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class FinanceApiTest extends IntegrationTest {

    private final LocalDate today = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
    private Cookie me;

    @BeforeEach
    void login() throws Exception {
        me = loginAs("eu@exemplo.com");
    }

    private String account(Cookie s, String name, String opening) throws Exception {
        return create(s, "/accounts", """
                {"name": "%s", "openingBalance": %s}""".formatted(name, opening));
    }

    private String card(Cookie s, String paymentAccount) throws Exception {
        return create(s, "/cards", """
                {"name": "Nubank", "creditLimit": 2000, "closingDay": 10, "dueDay": 20, "paymentAccountId": "%s"}"""
                .formatted(paymentAccount));
    }

    private void tx(Cookie s, String json) throws Exception {
        postJson(s, "/transactions", json).andExpect(status().isCreated());
    }

    private String categoryId(Cookie s, String name) throws Exception {
        String json = getJson(s, "/categories").andReturn().getResponse().getContentAsString();
        List<String> ids = JsonPath.read(json, "$[?(@.name == '%s')].id".formatted(name));
        return ids.getFirst();
    }

    @Test
    void saldoDasContasComPendentesETransferencia() throws Exception {
        String corrente = account(me, "Corrente", "1000");
        String poupanca = account(me, "Poupança", "0");
        tx(me, """
                {"kind": "RECEITA", "date": "2026-01-05", "description": "Salário", "amount": 500, "accountId": "%s"}"""
                .formatted(corrente));
        tx(me, """
                {"kind": "DESPESA", "date": "2026-01-06", "description": "Mercado", "amount": 200, "accountId": "%s"}"""
                .formatted(corrente));
        tx(me, """
                {"kind": "DESPESA", "date": "2026-01-20", "description": "Luz", "amount": 100, "accountId": "%s",
                 "paid": false}""".formatted(corrente));
        tx(me, """
                {"kind": "TRANSFERENCIA", "date": "2026-01-07", "description": "Guardar", "amount": 300,
                 "accountId": "%s", "toAccountId": "%s"}""".formatted(corrente, poupanca));

        getJson(me, "/accounts")
                .andExpect(jsonPath("$[?(@.name == 'Corrente')].balance").value(1000.0))
                .andExpect(jsonPath("$[?(@.name == 'Corrente')].projectedBalance").value(900.0))
                .andExpect(jsonPath("$[?(@.name == 'Poupança')].balance").value(300.0));

        // transferência não é receita nem despesa
        getJson(me, "/finance/summary?month=2026-01")
                .andExpect(jsonPath("$.income").value(500.0))
                .andExpect(jsonPath("$.expense").value(300.0))
                .andExpect(jsonPath("$.net").value(200.0))
                .andExpect(jsonPath("$.totalBalance").value(1300.0))
                .andExpect(jsonPath("$.pendingCount").value(1))
                .andExpect(jsonPath("$.pendingAmount").value(100.0))
                .andExpect(jsonPath("$.expenseByCategory[0].name").value("Sem categoria"));

        getJson(me, "/transactions?month=2026-01&accountId=" + poupanca).andExpect(jsonPath("$", hasSize(1)));
        getJson(me, "/finance/history?until=2026-02&months=3")
                .andExpect(jsonPath("$[*].month", contains("2025-12", "2026-01", "2026-02")))
                .andExpect(jsonPath("$[1].expense").value(300.0))
                .andExpect(jsonPath("$[2].income").value(0));
    }

    @Test
    void compraParceladaNoCartaoEFatura() throws Exception {
        String corrente = account(me, "Corrente", "1000");
        String nubank = card(me, corrente);

        postJson(me, "/transactions", """
                {"kind": "DESPESA", "date": "2026-01-05", "description": "Fone", "amount": 300, "cardId": "%s",
                 "installments": 3}""".formatted(nubank))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].invoiceMonth", contains("2026-01", "2026-02", "2026-03")))
                .andExpect(jsonPath("$[*].installmentNo", contains(1, 2, 3)))
                .andExpect(jsonPath("$[2].date").value("2026-03-05"));
        // depois do fechamento (dia 10): cai na fatura seguinte
        postJson(me, "/transactions", """
                {"kind": "DESPESA", "date": "2026-01-15", "description": "Livro", "amount": 50, "cardId": "%s"}"""
                .formatted(nubank))
                .andExpect(jsonPath("$[0].invoiceMonth").value("2026-02"));

        getJson(me, "/cards")
                .andExpect(jsonPath("$[0].used").value(350.0))
                .andExpect(jsonPath("$[0].available").value(1650.0));
        getJson(me, "/cards/" + nubank + "/invoices/2026-02")
                .andExpect(jsonPath("$.summary.total").value(150.0))
                .andExpect(jsonPath("$.summary.dueDate").value("2026-02-20"))
                .andExpect(jsonPath("$.items", hasSize(2)));

        postJson(me, "/cards/" + nubank + "/invoices/2026-01/pay", """
                {"accountId": "%s", "date": "2026-01-20"}""".formatted(corrente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.remaining").value(0))
                .andExpect(jsonPath("$.summary.status").value("PAGA"));

        // pagar a fatura tira da conta, mas não é despesa de novo
        getJson(me, "/accounts").andExpect(jsonPath("$[0].balance").value(900.0));
        getJson(me, "/finance/summary?month=2026-01").andExpect(jsonPath("$.expense").value(150.0));
        getJson(me, "/cards").andExpect(jsonPath("$[0].used").value(250.0));

        // apagar todas as parcelas
        String json = getJson(me, "/transactions?from=2026-03-01&to=2026-03-31").andReturn().getResponse()
                .getContentAsString();
        String third = JsonPath.read(json, "$[0].id");
        deleteJson(me, "/transactions/" + third + "?allInstallments=true").andExpect(status().isNoContent());
        getJson(me, "/transactions?from=2026-01-01&to=2026-03-31&cardId=" + nubank)
                .andExpect(jsonPath("$[*].description", contains("Fatura Nubank 2026-01", "Livro")));
    }

    @Test
    void regrasDosLancamentos() throws Exception {
        String corrente = account(me, "Corrente", "0");
        String outra = account(me, "Outra", "0");
        String nubank = card(me, corrente);
        postJson(me, "/categories/defaults", "").andExpect(status().isCreated());
        String salario = categoryId(me, "Salário");

        postJson(me, "/transactions", """
                {"kind": "RECEITA", "date": "2026-01-05", "description": "x", "amount": 10, "cardId": "%s"}"""
                .formatted(nubank)).andExpect(status().isBadRequest());
        postJson(me, "/transactions", """
                {"kind": "DESPESA", "date": "2026-01-05", "description": "x", "amount": 10}""")
                .andExpect(status().isBadRequest());
        postJson(me, "/transactions", """
                {"kind": "DESPESA", "date": "2026-01-05", "description": "x", "amount": 10, "accountId": "%s",
                 "categoryId": "%s"}""".formatted(corrente, salario))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("A categoria Salário é de receita, não de despesa"));
        postJson(me, "/transactions", """
                {"kind": "DESPESA", "date": "2026-01-05", "description": "x", "amount": 10, "accountId": "%s",
                 "installments": 2}""".formatted(corrente)).andExpect(status().isBadRequest());
        postJson(me, "/transactions", """
                {"kind": "TRANSFERENCIA", "date": "2026-01-05", "description": "x", "amount": 10,
                 "accountId": "%s", "toAccountId": "%s"}""".formatted(corrente, corrente))
                .andExpect(status().isBadRequest());
        postJson(me, "/transactions", """
                {"kind": "PAGAMENTO_FATURA", "date": "2026-01-05", "description": "x", "amount": 10,
                 "accountId": "%s", "cardId": "%s"}""".formatted(corrente, nubank))
                .andExpect(status().isBadRequest());
        postJson(me, "/transactions", """
                {"kind": "RECEITA", "date": "2026-01-05", "description": "x", "amount": 10.555, "accountId": "%s"}"""
                .formatted(corrente)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.amount").exists());

        // conta com lançamento não apaga; arquivada não recebe lançamento novo
        tx(me, """
                {"kind": "RECEITA", "date": "2026-01-05", "description": "x", "amount": 10, "accountId": "%s"}"""
                .formatted(outra));
        deleteJson(me, "/accounts/" + outra).andExpect(status().isConflict());
        putJson(me, "/accounts/" + outra, "{\"name\": \"Outra\", \"archived\": true}").andExpect(status().isOk());
        postJson(me, "/transactions", """
                {"kind": "RECEITA", "date": "2026-01-06", "description": "x", "amount": 10, "accountId": "%s"}"""
                .formatted(outra)).andExpect(status().isBadRequest());

        postJson(me, "/categories/defaults", "").andExpect(status().isConflict());
        postJson(me, "/accounts", "{\"name\": \"corrente\"}").andExpect(status().isConflict());
    }

    @Test
    void recorrenciaGeraSemDuplicar() throws Exception {
        String corrente = account(me, "Corrente", "0");
        LocalDate start = today.minusMonths(2);
        String id = create(me, "/recurrences", """
                {"description": "Aluguel", "amount": 1500, "kind": "DESPESA", "accountId": "%s",
                 "startsOn": "%s"}""".formatted(corrente, start));

        getJson(me, "/recurrences")
                .andExpect(jsonPath("$[0].generated").value(3))
                .andExpect(jsonPath("$[0].nextOn").value(start.plusMonths(3).toString()));
        String range = "?from=%s&to=%s".formatted(start, today);
        getJson(me, "/transactions" + range)
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].paid").value(false))
                .andExpect(jsonPath("$[0].recurrenceId").value(id));

        // mudar o valor não gera nada; mudar a data recomeça depois de hoje
        putJson(me, "/recurrences/" + id, """
                {"description": "Aluguel", "amount": 1600, "kind": "DESPESA", "accountId": "%s",
                 "startsOn": "%s"}""".formatted(corrente, start)).andExpect(status().isOk());
        putJson(me, "/recurrences/" + id, """
                {"description": "Aluguel", "amount": 1600, "kind": "DESPESA", "accountId": "%s",
                 "startsOn": "%s"}""".formatted(corrente, start.minusDays(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextOn").value(org.hamcrest.Matchers.greaterThan(today.toString())));
        getJson(me, "/transactions" + range).andExpect(jsonPath("$", hasSize(3)));

        postJson(me, "/recurrences/" + id + "/pause", "").andExpect(jsonPath("$.active").value(false));
        deleteJson(me, "/recurrences/" + id).andExpect(status().isNoContent());
        getJson(me, "/transactions" + range)
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].recurrenceId").doesNotExist());
    }

    @Test
    void orcamentoComSubcategoria() throws Exception {
        String corrente = account(me, "Corrente", "0");
        postJson(me, "/categories/defaults", "").andExpect(status().isCreated());
        String alimentacao = categoryId(me, "Alimentação");
        String mercado = create(me, "/categories", """
                {"name": "Mercado", "kind": "DESPESA", "parentId": "%s"}""".formatted(alimentacao));
        postJson(me, "/categories", """
                {"name": "Sub da sub", "kind": "DESPESA", "parentId": "%s"}""".formatted(mercado))
                .andExpect(status().isBadRequest());

        putJson(me, "/budgets", """
                {"categoryId": "%s", "month": "2026-01", "limitAmount": 500}""".formatted(alimentacao))
                .andExpect(status().isOk());
        tx(me, """
                {"kind": "DESPESA", "date": "2026-01-10", "description": "Compra do mês", "amount": 450,
                 "accountId": "%s", "categoryId": "%s"}""".formatted(corrente, mercado));

        getJson(me, "/budgets?month=2026-01")
                .andExpect(jsonPath("$[0].spent").value(450.0))
                .andExpect(jsonPath("$[0].percent").value(90))
                .andExpect(jsonPath("$[0].status").value("ATENCAO"));
        getJson(me, "/finance/summary?month=2026-01")
                .andExpect(jsonPath("$.expenseByCategory[0].name").value("Alimentação"));
        getJson(me, "/transactions?month=2026-01&categoryId=" + alimentacao).andExpect(jsonPath("$", hasSize(1)));

        postJson(me, "/budgets/copy?from=2026-01&to=2026-02", "")
                .andExpect(jsonPath("$[0].limitAmount").value(500.0))
                .andExpect(jsonPath("$[0].spent").value(0));
        putJson(me, "/budgets", """
                {"categoryId": "%s", "month": "2026-01", "limitAmount": 100}""".formatted(categoryId(me, "Salário")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void outroUsuarioNaoMexeNoQueEhMeu() throws Exception {
        String corrente = account(me, "Corrente", "100");
        String nubank = card(me, corrente);
        Cookie other = loginAs("outro@exemplo.com");
        String otherAccount = account(other, "Minha", "0");

        getJson(other, "/accounts").andExpect(jsonPath("$[*].name", contains("Minha")));
        postJson(other, "/transactions", """
                {"kind": "RECEITA", "date": "2026-01-05", "description": "x", "amount": 10, "accountId": "%s"}"""
                .formatted(corrente)).andExpect(status().isNotFound());
        postJson(other, "/transactions", """
                {"kind": "TRANSFERENCIA", "date": "2026-01-05", "description": "x", "amount": 10,
                 "accountId": "%s", "toAccountId": "%s"}""".formatted(otherAccount, corrente))
                .andExpect(status().isNotFound());
        postJson(other, "/cards/" + nubank + "/invoices/2026-01/pay", """
                {"accountId": "%s"}""".formatted(otherAccount)).andExpect(status().isNotFound());
        getJson(other, "/cards/" + nubank + "/invoices/2026-01").andExpect(status().isNotFound());

        UUID otherUser = jdbc.queryForObject("select id from users where email = 'outro@exemplo.com'", UUID.class);
        assertThatThrownBy(() -> jdbc.update("""
                insert into transactions (user_id, date, description, amount, kind, account_id)
                values (?, '2026-01-05', 'x', 10, 'RECEITA', ?::uuid)""", otherUser, corrente))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("select count(*) from transactions", Integer.class)).isZero();
    }

    @Test
    void mesInvalido() throws Exception {
        getJson(me, "/finance/summary?month=2026-13").andExpect(status().isBadRequest());
        getJson(me, "/finance/summary").andExpect(jsonPath("$.month").value(YearMonth.from(today).toString()));
    }
}
