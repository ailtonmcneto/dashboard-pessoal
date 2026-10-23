package com.vida.financas;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Somas que alimentam saldos, faturas, orçamento e relatórios. SQL direto porque são agregações;
 * toda consulta filtra por {@code user_id}.
 */
@Component
class FinanceQueries {

    record Balance(BigDecimal paid, BigDecimal projected) {
    }

    record InvoiceTotals(BigDecimal purchases, BigDecimal payments) {
    }

    record Totals(BigDecimal income, BigDecimal expense) {
    }

    record Pending(int count, BigDecimal amount) {
    }

    private final JdbcTemplate jdbc;

    FinanceQueries(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Saldo por conta: abertura + entradas − saídas (transferência conta dos dois lados). */
    Map<UUID, Balance> accountBalances(UUID userId) {
        Map<UUID, Balance> result = new HashMap<>();
        jdbc.query("""
                select a.id,
                       a.opening_balance + coalesce(sum(m.signed) filter (where m.paid), 0) as paid,
                       a.opening_balance + coalesce(sum(m.signed), 0) as projected
                  from accounts a
                  left join lateral (
                        select case when (t.kind = 'RECEITA' and t.account_id = a.id)
                                      or (t.kind = 'TRANSFERENCIA' and t.to_account_id = a.id)
                                    then t.amount else -t.amount end as signed,
                               t.paid
                          from transactions t
                         where t.user_id = a.user_id and (t.account_id = a.id or t.to_account_id = a.id)
                  ) m on true
                 where a.user_id = ?
                 group by a.id, a.opening_balance
                """, rs -> {
                    result.put(rs.getObject("id", UUID.class),
                            new Balance(rs.getBigDecimal("paid"), rs.getBigDecimal("projected")));
                }, userId);
        return result;
    }

    /** Em aberto por cartão: compras − pagamentos de fatura (todas as faturas). */
    Map<UUID, BigDecimal> cardOutstanding(UUID userId) {
        Map<UUID, BigDecimal> result = new HashMap<>();
        jdbc.query("""
                select card_id,
                       coalesce(sum(amount) filter (where kind = 'DESPESA'), 0)
                     - coalesce(sum(amount) filter (where kind = 'PAGAMENTO_FATURA'), 0) as outstanding
                  from transactions
                 where user_id = ? and card_id is not null
                 group by card_id
                """, rs -> {
                    result.put(rs.getObject("card_id", UUID.class), rs.getBigDecimal("outstanding"));
                }, userId);
        return result;
    }

    InvoiceTotals invoice(UUID userId, UUID cardId, YearMonth month) {
        return jdbc.queryForObject("""
                select coalesce(sum(amount) filter (where kind = 'DESPESA'), 0) as purchases,
                       coalesce(sum(amount) filter (where kind = 'PAGAMENTO_FATURA'), 0) as payments
                  from transactions
                 where user_id = ? and card_id = ? and invoice_month = ?
                """, (rs, i) -> new InvoiceTotals(rs.getBigDecimal("purchases"), rs.getBigDecimal("payments")),
                userId, cardId, month.atDay(1));
    }

    /** Receitas e despesas por mês no intervalo [from, to] (meses sem movimento ficam de fora). */
    Map<YearMonth, Totals> monthTotals(UUID userId, YearMonth from, YearMonth to) {
        Map<YearMonth, Totals> result = new LinkedHashMap<>();
        jdbc.query("""
                select date_trunc('month', date)::date as month,
                       coalesce(sum(amount) filter (where kind = 'RECEITA'), 0) as income,
                       coalesce(sum(amount) filter (where kind = 'DESPESA'), 0) as expense
                  from transactions
                 where user_id = ? and date >= ? and date <= ? and kind in ('RECEITA', 'DESPESA')
                 group by 1 order by 1
                """, rs -> {
                    result.put(YearMonth.from(rs.getObject("month", LocalDate.class)),
                            new Totals(rs.getBigDecimal("income"), rs.getBigDecimal("expense")));
                }, userId, from.atDay(1), to.atEndOfMonth());
        return result;
    }

    /** Soma por categoria (a própria, não a raiz) de um tipo no mês; chave null = sem categoria. */
    Map<UUID, BigDecimal> byCategory(UUID userId, YearMonth month, Transaction.Kind kind) {
        Map<UUID, BigDecimal> result = new HashMap<>();
        jdbc.query("""
                select category_id, sum(amount) as total
                  from transactions
                 where user_id = ? and kind = ? and date >= ? and date <= ?
                 group by category_id
                """, rs -> {
                    result.put(rs.getObject("category_id", UUID.class), rs.getBigDecimal("total"));
                }, userId, kind.name(), month.atDay(1), month.atEndOfMonth());
        return result;
    }

    /** Contas a pagar: despesas em conta ainda não pagas, com data até {@code until}. */
    Pending pendingExpenses(UUID userId, LocalDate until) {
        return jdbc.queryForObject("""
                select count(*) as n, coalesce(sum(amount), 0) as total
                  from transactions
                 where user_id = ? and kind = 'DESPESA' and account_id is not null and not paid and date <= ?
                """, (rs, i) -> new Pending(rs.getInt("n"), rs.getBigDecimal("total")), userId, until);
    }
}
