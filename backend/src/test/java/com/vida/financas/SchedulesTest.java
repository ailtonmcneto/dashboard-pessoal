package com.vida.financas;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class SchedulesTest {

    @Test
    void mensalNaoPerdeODia31() {
        LocalDate start = LocalDate.of(2026, 1, 31);
        assertThat(Schedules.occurrence(start, Recurrence.Frequency.MENSAL, 1)).isEqualTo("2026-02-28");
        assertThat(Schedules.occurrence(start, Recurrence.Frequency.MENSAL, 2)).isEqualTo("2026-03-31");
        assertThat(Schedules.occurrence(start, Recurrence.Frequency.SEMANAL, 2)).isEqualTo("2026-02-14");
        assertThat(Schedules.occurrence(start, Recurrence.Frequency.ANUAL, 1)).isEqualTo("2027-01-31");
    }

    @Test
    void faturaDependeDoDiaDeFechamento() {
        assertThat(Schedules.invoiceFor(LocalDate.of(2026, 1, 9), 10)).isEqualTo(YearMonth.of(2026, 1));
        assertThat(Schedules.invoiceFor(LocalDate.of(2026, 1, 10), 10)).isEqualTo(YearMonth.of(2026, 2));
        assertThat(Schedules.invoiceFor(LocalDate.of(2026, 12, 20), 10)).isEqualTo(YearMonth.of(2027, 1));
    }

    @Test
    void vencimentoNoMesmoMesOuNoSeguinte() {
        YearMonth jan = YearMonth.of(2026, 1);
        assertThat(Schedules.dueDate(jan, 3, 10)).isEqualTo("2026-01-10");
        assertThat(Schedules.dueDate(jan, 25, 5)).isEqualTo("2026-02-05");
    }

    @Test
    void parcelasSomamOTotalComCentavosNaPrimeira() {
        assertThat(Schedules.split(new BigDecimal("100.00"), 3))
                .containsExactly(new BigDecimal("33.34"), new BigDecimal("33.33"), new BigDecimal("33.33"));
        assertThat(Schedules.split(new BigDecimal("300.00"), 3)).allMatch(p -> p.compareTo(new BigDecimal("100")) == 0);
        assertThat(Schedules.split(new BigDecimal("0.05"), 4))
                .containsExactly(new BigDecimal("0.02"), new BigDecimal("0.01"), new BigDecimal("0.01"),
                        new BigDecimal("0.01"));
    }
}
