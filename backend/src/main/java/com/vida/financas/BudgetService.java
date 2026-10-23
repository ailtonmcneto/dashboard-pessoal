package com.vida.financas;

import com.vida.common.BusinessException;
import com.vida.common.CurrentUser;
import com.vida.common.NotFoundException;
import com.vida.financas.FinanceDtos.BudgetRequest;
import com.vida.financas.FinanceDtos.BudgetResponse;
import com.vida.financas.FinanceDtos.BudgetStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Limite mensal por categoria de despesa. O gasto da categoria inclui as subcategorias. */
@Service
public class BudgetService {

    private final BudgetRepository budgets;
    private final CategoryService categories;
    private final FinanceQueries queries;
    private final CurrentUser currentUser;

    BudgetService(BudgetRepository budgets, CategoryService categories, FinanceQueries queries,
            CurrentUser currentUser) {
        this.budgets = budgets;
        this.categories = categories;
        this.queries = queries;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> list(YearMonth month) {
        UUID userId = currentUser.id();
        Map<UUID, BigDecimal> spentByCategory = queries.byCategory(userId, month, Transaction.Kind.DESPESA);
        Map<UUID, Category> all = categories.byId();
        return budgets.findByUserIdAndMonth(userId, month.atDay(1)).stream()
                .map(b -> toResponse(b, spent(b.getCategoryId(), spentByCategory, all)))
                .sorted(Comparator.comparingInt(BudgetResponse::percent).reversed())
                .toList();
    }

    /** Cria ou atualiza o limite da categoria naquele mês. */
    @Transactional
    public BudgetResponse upsert(BudgetRequest req) {
        Category category = categories.find(req.categoryId());
        if (category.getKind() != Category.Kind.DESPESA) {
            throw BusinessException.badRequest("Orçamento é só para categorias de despesa");
        }
        UUID userId = currentUser.id();
        Budget budget = budgets.findByUserIdAndCategoryIdAndMonth(userId, category.getId(), req.month().atDay(1))
                .orElseGet(() -> new Budget(userId, category.getId(), req.month().atDay(1)));
        budget.setLimitAmount(req.limitAmount());
        budgets.save(budget);
        return list(req.month()).stream().filter(r -> r.id().equals(budget.getId())).findFirst().orElseThrow();
    }

    /** Copia os limites de um mês para outro (sem sobrescrever os que já existem no destino). */
    @Transactional
    public List<BudgetResponse> copy(YearMonth from, YearMonth to) {
        if (from.equals(to)) {
            throw BusinessException.badRequest("Escolha meses diferentes");
        }
        UUID userId = currentUser.id();
        for (Budget source : budgets.findByUserIdAndMonth(userId, from.atDay(1))) {
            if (budgets.findByUserIdAndCategoryIdAndMonth(userId, source.getCategoryId(), to.atDay(1)).isEmpty()) {
                Budget copy = new Budget(userId, source.getCategoryId(), to.atDay(1));
                copy.setLimitAmount(source.getLimitAmount());
                budgets.save(copy);
            }
        }
        return list(to);
    }

    @Transactional
    public void delete(UUID id) {
        budgets.delete(budgets.findByIdAndUserId(id, currentUser.id())
                .orElseThrow(() -> new NotFoundException("Orçamento")));
    }

    private static BigDecimal spent(UUID categoryId, Map<UUID, BigDecimal> byCategory, Map<UUID, Category> all) {
        BigDecimal total = byCategory.getOrDefault(categoryId, BigDecimal.ZERO);
        for (Category c : all.values()) {
            if (categoryId.equals(c.getParentId())) {
                total = total.add(byCategory.getOrDefault(c.getId(), BigDecimal.ZERO));
            }
        }
        return total;
    }

    static BudgetResponse toResponse(Budget b, BigDecimal spent) {
        int percent = spent.multiply(BigDecimal.valueOf(100))
                .divide(b.getLimitAmount(), 0, RoundingMode.HALF_UP).intValue();
        BudgetStatus status = percent > 100 ? BudgetStatus.ESTOURADO
                : percent >= 80 ? BudgetStatus.ATENCAO : BudgetStatus.OK;
        return new BudgetResponse(b.getId(), b.getCategoryId(), YearMonth.from(b.getMonth()), b.getLimitAmount(),
                spent, percent, status);
    }
}
