package com.vida.financas;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Contrato HTTP de finanças. Valores em reais com 2 casas; meses no formato AAAA-MM. */
public final class FinanceDtos {

    private FinanceDtos() {
    }

    // ---------- Contas ----------

    public record AccountRequest(
            @NotBlank @Size(max = 60) String name,
            Account.@Nullable Kind kind,
            @Digits(integer = 12, fraction = 2) @Nullable BigDecimal openingBalance,
            @Nullable Boolean archived) {
    }

    /** {@code balance}: só o que já foi pago/recebido; {@code projectedBalance}: inclui pendentes. */
    public record AccountResponse(UUID id, String name, Account.Kind kind, BigDecimal openingBalance,
            BigDecimal balance, BigDecimal projectedBalance, boolean archived) {
    }

    // ---------- Cartões ----------

    public record CardRequest(
            @NotBlank @Size(max = 60) String name,
            @Positive @Digits(integer = 12, fraction = 2) @Nullable BigDecimal creditLimit,
            @NotNull @Min(1) @Max(28) Integer closingDay,
            @NotNull @Min(1) @Max(28) Integer dueDay,
            @Nullable UUID paymentAccountId,
            @Nullable Boolean archived) {
    }

    public enum InvoiceStatus { ABERTA, FECHADA, ATRASADA, PAGA }

    public record InvoiceSummary(YearMonth month, LocalDate closingDate, LocalDate dueDate, BigDecimal total,
            BigDecimal paidAmount, BigDecimal remaining, InvoiceStatus status) {
    }

    /** {@code used}: tudo que foi comprado e ainda não foi pago (todas as faturas, inclusive parcelas futuras). */
    public record CardResponse(UUID id, String name, @Nullable BigDecimal creditLimit, int closingDay, int dueDay,
            @Nullable UUID paymentAccountId, boolean archived, BigDecimal used, @Nullable BigDecimal available,
            InvoiceSummary currentInvoice) {
    }

    public record InvoiceResponse(UUID cardId, InvoiceSummary summary, List<TransactionResponse> items) {
    }

    public record PayInvoiceRequest(
            @NotNull UUID accountId,
            @Positive @Digits(integer = 12, fraction = 2) @Nullable BigDecimal amount,
            @Nullable LocalDate date) {
    }

    // ---------- Categorias ----------

    public record CategoryRequest(
            @NotBlank @Size(max = 60) String name,
            @NotNull Category.Kind kind,
            @Nullable UUID parentId,
            @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "use uma cor no formato #RRGGBB") @Nullable String color,
            @Nullable Boolean archived) {
    }

    public record CategoryResponse(UUID id, String name, Category.Kind kind, @Nullable UUID parentId, String color,
            boolean archived) {
    }

    // ---------- Lançamentos ----------

    public record TransactionRequest(
            @NotNull Transaction.Kind kind,
            @NotNull LocalDate date,
            @NotBlank @Size(max = 120) String description,
            @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal amount,
            @Nullable UUID categoryId,
            @Nullable UUID accountId,
            @Nullable UUID cardId,
            @Nullable UUID toAccountId,
            @Min(1) @Max(48) @Nullable Integer installments,
            @Nullable Boolean paid,
            @Size(max = 500) @Nullable String notes) {
    }

    public record TransactionResponse(
            UUID id, Transaction.Kind kind, LocalDate date, String description, BigDecimal amount,
            @Nullable UUID categoryId, @Nullable UUID accountId, @Nullable UUID cardId, @Nullable UUID toAccountId,
            @Nullable YearMonth invoiceMonth, boolean paid, @Nullable UUID installmentGroupId,
            @Nullable Integer installmentNo, @Nullable Integer installmentTotal, @Nullable UUID recurrenceId,
            @Nullable String notes) {
    }

    // ---------- Recorrências ----------

    public record RecurrenceRequest(
            @NotBlank @Size(max = 120) String description,
            @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal amount,
            @NotNull Transaction.Kind kind,
            @Nullable UUID categoryId,
            @Nullable UUID accountId,
            @Nullable UUID cardId,
            Recurrence.@Nullable Frequency frequency,
            @NotNull LocalDate startsOn,
            @Nullable LocalDate endsOn) {
    }

    public record RecurrenceResponse(
            UUID id, String description, BigDecimal amount, Transaction.Kind kind, @Nullable UUID categoryId,
            @Nullable UUID accountId, @Nullable UUID cardId, Recurrence.Frequency frequency, LocalDate startsOn,
            @Nullable LocalDate endsOn, LocalDate nextOn, boolean active, int generated) {
    }

    // ---------- Orçamento ----------

    public record BudgetRequest(
            @NotNull UUID categoryId,
            @NotNull YearMonth month,
            @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal limitAmount) {
    }

    public enum BudgetStatus { OK, ATENCAO, ESTOURADO }

    /** Gasto inclui as subcategorias. ATENCAO a partir de 80% do limite. */
    public record BudgetResponse(UUID id, UUID categoryId, YearMonth month, BigDecimal limitAmount, BigDecimal spent,
            int percent, BudgetStatus status) {
    }

    // ---------- Resumos ----------

    public record CategoryTotal(@Nullable UUID categoryId, String name, String color, BigDecimal amount) {
    }

    /** Receitas e despesas do mês (sem transferências nem pagamento de fatura), por categoria principal. */
    public record MonthSummary(YearMonth month, BigDecimal income, BigDecimal expense, BigDecimal net,
            List<CategoryTotal> expenseByCategory, List<CategoryTotal> incomeByCategory,
            BigDecimal totalBalance, int pendingCount, BigDecimal pendingAmount) {
    }

    public record MonthTotals(YearMonth month, BigDecimal income, BigDecimal expense) {
    }
}
