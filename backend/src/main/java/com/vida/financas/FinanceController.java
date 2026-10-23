package com.vida.financas;

import com.vida.common.BusinessException;
import com.vida.common.CurrentUser;
import com.vida.financas.FinanceDtos.AccountRequest;
import com.vida.financas.FinanceDtos.AccountResponse;
import com.vida.financas.FinanceDtos.BudgetRequest;
import com.vida.financas.FinanceDtos.BudgetResponse;
import com.vida.financas.FinanceDtos.CardRequest;
import com.vida.financas.FinanceDtos.CardResponse;
import com.vida.financas.FinanceDtos.CategoryRequest;
import com.vida.financas.FinanceDtos.CategoryResponse;
import com.vida.financas.FinanceDtos.InvoiceResponse;
import com.vida.financas.FinanceDtos.MonthSummary;
import com.vida.financas.FinanceDtos.MonthTotals;
import com.vida.financas.FinanceDtos.PayInvoiceRequest;
import com.vida.financas.FinanceDtos.RecurrenceRequest;
import com.vida.financas.FinanceDtos.RecurrenceResponse;
import com.vida.financas.FinanceDtos.TransactionRequest;
import com.vida.financas.FinanceDtos.TransactionResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** API de finanças. Meses em query string no formato AAAA-MM; sem mês, o atual (no fuso do usuário). */
@RestController
class FinanceController {

    private final AccountService accounts;
    private final CardService cards;
    private final CategoryService categories;
    private final TransactionService transactions;
    private final RecurrenceService recurrences;
    private final BudgetService budgets;
    private final ReportService reports;
    private final CurrentUser currentUser;

    FinanceController(AccountService accounts, CardService cards, CategoryService categories,
            TransactionService transactions, RecurrenceService recurrences, BudgetService budgets,
            ReportService reports, CurrentUser currentUser) {
        this.accounts = accounts;
        this.cards = cards;
        this.categories = categories;
        this.transactions = transactions;
        this.recurrences = recurrences;
        this.budgets = budgets;
        this.reports = reports;
        this.currentUser = currentUser;
    }

    private YearMonth month(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return YearMonth.from(currentUser.today());
        }
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            throw BusinessException.badRequest("Mês inválido: use AAAA-MM");
        }
    }

    // ---------- Contas ----------

    @GetMapping("/accounts")
    List<AccountResponse> accounts() {
        return accounts.list();
    }

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    AccountResponse createAccount(@Valid @RequestBody AccountRequest req) {
        return accounts.create(req);
    }

    @PutMapping("/accounts/{id}")
    AccountResponse updateAccount(@PathVariable UUID id, @Valid @RequestBody AccountRequest req) {
        return accounts.update(id, req);
    }

    @DeleteMapping("/accounts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteAccount(@PathVariable UUID id) {
        accounts.delete(id);
    }

    // ---------- Cartões e faturas ----------

    @GetMapping("/cards")
    List<CardResponse> cards() {
        return cards.list();
    }

    @PostMapping("/cards")
    @ResponseStatus(HttpStatus.CREATED)
    CardResponse createCard(@Valid @RequestBody CardRequest req) {
        return cards.create(req);
    }

    @PutMapping("/cards/{id}")
    CardResponse updateCard(@PathVariable UUID id, @Valid @RequestBody CardRequest req) {
        return cards.update(id, req);
    }

    @DeleteMapping("/cards/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteCard(@PathVariable UUID id) {
        cards.delete(id);
    }

    @GetMapping("/cards/{id}/invoices/{month}")
    InvoiceResponse invoice(@PathVariable UUID id, @PathVariable String month) {
        return cards.invoice(id, month(month));
    }

    @PostMapping("/cards/{id}/invoices/{month}/pay")
    InvoiceResponse payInvoice(@PathVariable UUID id, @PathVariable String month,
            @Valid @RequestBody PayInvoiceRequest req) {
        return cards.pay(id, month(month), req);
    }

    // ---------- Categorias ----------

    @GetMapping("/categories")
    List<CategoryResponse> categories() {
        return categories.list();
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    CategoryResponse createCategory(@Valid @RequestBody CategoryRequest req) {
        return categories.create(req);
    }

    /** Cria o conjunto sugerido de categorias (só para quem ainda não tem nenhuma). */
    @PostMapping("/categories/defaults")
    @ResponseStatus(HttpStatus.CREATED)
    List<CategoryResponse> createDefaultCategories() {
        return categories.createDefaults();
    }

    @PutMapping("/categories/{id}")
    CategoryResponse updateCategory(@PathVariable UUID id, @Valid @RequestBody CategoryRequest req) {
        return categories.update(id, req);
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteCategory(@PathVariable UUID id) {
        categories.delete(id);
    }

    // ---------- Lançamentos ----------

    /** Por padrão, o mês atual. {@code month} ou {@code from}/{@code to} (datas) escolhem o período. */
    @GetMapping("/transactions")
    List<TransactionResponse> transactions(@RequestParam(required = false) @Nullable String month,
            @RequestParam(required = false) @Nullable LocalDate from,
            @RequestParam(required = false) @Nullable LocalDate to,
            @RequestParam(required = false) @Nullable UUID accountId,
            @RequestParam(required = false) @Nullable UUID cardId,
            @RequestParam(required = false) @Nullable UUID categoryId) {
        LocalDate start;
        LocalDate end;
        if (from != null || to != null) {
            if (from == null || to == null) {
                throw BusinessException.badRequest("Informe 'from' e 'to' juntos");
            }
            start = from;
            end = to;
        } else {
            YearMonth m = month(month);
            start = m.atDay(1);
            end = m.atEndOfMonth();
        }
        return transactions.list(start, end, accountId, cardId, categoryId);
    }

    /** Devolve a lista criada: uma compra parcelada gera um lançamento por parcela. */
    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    List<TransactionResponse> createTransaction(@Valid @RequestBody TransactionRequest req) {
        return transactions.create(req);
    }

    @PutMapping("/transactions/{id}")
    TransactionResponse updateTransaction(@PathVariable UUID id, @Valid @RequestBody TransactionRequest req) {
        return transactions.update(id, req);
    }

    @DeleteMapping("/transactions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteTransaction(@PathVariable UUID id, @RequestParam(defaultValue = "false") boolean allInstallments) {
        transactions.delete(id, allInstallments);
    }

    // ---------- Recorrências ----------

    @GetMapping("/recurrences")
    List<RecurrenceResponse> recurrences() {
        return recurrences.list();
    }

    @PostMapping("/recurrences")
    @ResponseStatus(HttpStatus.CREATED)
    RecurrenceResponse createRecurrence(@Valid @RequestBody RecurrenceRequest req) {
        return recurrences.create(req);
    }

    @PutMapping("/recurrences/{id}")
    RecurrenceResponse updateRecurrence(@PathVariable UUID id, @Valid @RequestBody RecurrenceRequest req) {
        return recurrences.update(id, req);
    }

    @PostMapping("/recurrences/{id}/pause")
    RecurrenceResponse pauseRecurrence(@PathVariable UUID id) {
        return recurrences.setActive(id, false);
    }

    @PostMapping("/recurrences/{id}/resume")
    RecurrenceResponse resumeRecurrence(@PathVariable UUID id) {
        return recurrences.setActive(id, true);
    }

    @DeleteMapping("/recurrences/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteRecurrence(@PathVariable UUID id) {
        recurrences.delete(id);
    }

    // ---------- Orçamento ----------

    @GetMapping("/budgets")
    List<BudgetResponse> budgets(@RequestParam(required = false) @Nullable String month) {
        return budgets.list(month(month));
    }

    /** Cria ou atualiza o limite (categoria + mês). */
    @PutMapping("/budgets")
    BudgetResponse upsertBudget(@Valid @RequestBody BudgetRequest req) {
        return budgets.upsert(req);
    }

    @PostMapping("/budgets/copy")
    List<BudgetResponse> copyBudgets(@RequestParam String from, @RequestParam String to) {
        return budgets.copy(month(from), month(to));
    }

    @DeleteMapping("/budgets/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteBudget(@PathVariable UUID id) {
        budgets.delete(id);
    }

    // ---------- Resumos ----------

    @GetMapping("/finance/summary")
    MonthSummary summary(@RequestParam(required = false) @Nullable String month) {
        return reports.summary(month(month));
    }

    @GetMapping("/finance/history")
    List<MonthTotals> history(@RequestParam(required = false) @Nullable String until,
            @RequestParam(defaultValue = "6") int months) {
        return reports.history(month(until), months);
    }
}
