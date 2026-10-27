package com.vida.financas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Regras de calendário das finanças: ocorrências, faturas e parcelas. Sem estado, fácil de testar. */
final class Schedules {

    private Schedules() {
    }

    /**
     * N-ésima ocorrência (0 = a primeira) contada sempre a partir do início, para não "perder" o dia:
     * mensal começando em 31/01 → 28/02 → 31/03 (e não 28/03).
     */
    static LocalDate occurrence(LocalDate startsOn, Recurrence.Frequency frequency, int n) {
        return switch (frequency) {
            case SEMANAL -> startsOn.plusWeeks(n);
            case MENSAL -> startsOn.plusMonths(n);
            case ANUAL -> startsOn.plusYears(n);
        };
    }

    /**
     * Fatura (identificada pelo mês em que fecha) de uma compra: compras até o dia anterior ao
     * fechamento entram na fatura do mês; do dia do fechamento em diante, na do mês seguinte.
     */
    static YearMonth invoiceFor(LocalDate purchase, int closingDay) {
        YearMonth month = YearMonth.from(purchase);
        return purchase.getDayOfMonth() >= closingDay ? month.plusMonths(1) : month;
    }

    static LocalDate closingDate(YearMonth invoice, int closingDay) {
        return invoice.atDay(closingDay);
    }

    /** Vencimento: no mesmo mês do fechamento se o dia for depois dele; senão, no mês seguinte. */
    static LocalDate dueDate(YearMonth invoice, int closingDay, int dueDay) {
        return dueDay > closingDay ? invoice.atDay(dueDay) : invoice.plusMonths(1).atDay(dueDay);
    }

    /** Divide em parcelas iguais; os centavos que sobram vão para a primeira. */
    static List<BigDecimal> split(BigDecimal total, int parts) {
        BigDecimal base = total.divide(BigDecimal.valueOf(parts), 2, RoundingMode.DOWN);
        BigDecimal first = total.subtract(base.multiply(BigDecimal.valueOf(parts - 1L)));
        List<BigDecimal> result = new ArrayList<>(parts);
        result.add(first);
        for (int i = 1; i < parts; i++) {
            result.add(base);
        }
        return result;
    }
}
