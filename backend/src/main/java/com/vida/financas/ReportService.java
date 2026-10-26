package com.vida.financas;

import com.vida.common.BusinessException;
import com.vida.common.CurrentUser;
import com.vida.financas.FinanceDtos.CategoryTotal;
import com.vida.financas.FinanceDtos.MonthSummary;
import com.vida.financas.FinanceDtos.MonthTotals;
import com.vida.financas.FinanceDtos.OverdueBills;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

    static final String NO_CATEGORY = "Sem categoria";
    static final String NO_CATEGORY_COLOR = "#a8a29e";

    private final FinanceQueries queries;
    private final CategoryService categories;
    private final CurrentUser currentUser;

    ReportService(FinanceQueries queries, CategoryService categories, CurrentUser currentUser) {
        this.queries = queries;
        this.categories = categories;
        this.currentUser = currentUser;
    }

    /** Receitas e despesas do mês, por categoria principal, mais saldo total e contas a pagar. */
    @Transactional(readOnly = true)
    public MonthSummary summary(YearMonth month) {
        UUID userId = currentUser.id();
        Map<UUID, Category> all = categories.byId();
        List<CategoryTotal> expenses = byRoot(queries.byCategory(userId, month, Transaction.Kind.DESPESA), all);
        List<CategoryTotal> incomes = byRoot(queries.byCategory(userId, month, Transaction.Kind.RECEITA), all);
        BigDecimal expense = sum(expenses);
        BigDecimal income = sum(incomes);
        BigDecimal totalBalance = queries.accountBalances(userId).values().stream()
                .map(FinanceQueries.Balance::paid).reduce(BigDecimal.ZERO, BigDecimal::add);
        FinanceQueries.Pending pending = queries.pendingExpenses(userId, month.atEndOfMonth());
        return new MonthSummary(month, income, expense, income.subtract(expense), expenses, incomes, totalBalance,
                pending.count(), pending.amount());
    }

    /** Contas vencidas: despesas em conta não pagas com data anterior a hoje. */
    @Transactional(readOnly = true)
    public OverdueBills overdueBills() {
        FinanceQueries.Pending p = queries.pendingExpenses(currentUser.id(), currentUser.today().minusDays(1));
        return new OverdueBills(p.count(), p.amount());
    }

    /** Receitas e despesas dos últimos {@code months} meses até {@code until} (meses vazios com zero). */
    @Transactional(readOnly = true)
    public List<MonthTotals> history(YearMonth until, int months) {
        if (months < 1 || months > 24) {
            throw BusinessException.badRequest("months deve estar entre 1 e 24");
        }
        YearMonth from = until.minusMonths(months - 1L);
        Map<YearMonth, FinanceQueries.Totals> totals = queries.monthTotals(currentUser.id(), from, until);
        List<MonthTotals> result = new ArrayList<>();
        for (YearMonth m = from; !m.isAfter(until); m = m.plusMonths(1)) {
            FinanceQueries.Totals t = totals.get(m);
            result.add(new MonthTotals(m, t == null ? BigDecimal.ZERO : t.income(),
                    t == null ? BigDecimal.ZERO : t.expense()));
        }
        return result;
    }

    /** Soma subcategorias na categoria principal; maior primeiro. */
    private static List<CategoryTotal> byRoot(Map<UUID, BigDecimal> byCategory, Map<UUID, Category> all) {
        Map<UUID, BigDecimal> roots = new HashMap<>();
        BigDecimal uncategorized = BigDecimal.ZERO;
        for (var e : byCategory.entrySet()) {
            Category c = e.getKey() == null ? null : all.get(e.getKey());
            if (c == null) {
                uncategorized = uncategorized.add(e.getValue());
            } else {
                UUID root = c.getParentId() != null ? c.getParentId() : c.getId();
                roots.merge(root, e.getValue(), BigDecimal::add);
            }
        }
        List<CategoryTotal> result = new ArrayList<>();
        roots.forEach((id, amount) -> {
            Category c = all.get(id);
            result.add(new CategoryTotal(id, c.getName(), c.getColor(), amount));
        });
        if (uncategorized.signum() > 0) {
            result.add(new CategoryTotal(null, NO_CATEGORY, NO_CATEGORY_COLOR, uncategorized));
        }
        result.sort(Comparator.comparing(CategoryTotal::amount).reversed());
        return result;
    }

    private static BigDecimal sum(List<CategoryTotal> totals) {
        return totals.stream().map(CategoryTotal::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
