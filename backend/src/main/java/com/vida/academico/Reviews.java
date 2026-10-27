package com.vida.academico;

import java.time.LocalDate;

/**
 * Revisão espaçada simples: quanto maior o domínio declarado (0–5), mais longe a próxima revisão.
 * 0 → amanhã, 1 → 2 dias, 2 → 4, 3 → 7, 4 → 15, 5 → 30.
 */
final class Reviews {

    private static final int[] INTERVAL_DAYS = {1, 2, 4, 7, 15, 30};

    private Reviews() {
    }

    static LocalDate nextReview(int mastery, LocalDate reviewedOn) {
        return reviewedOn.plusDays(INTERVAL_DAYS[mastery]);
    }
}
