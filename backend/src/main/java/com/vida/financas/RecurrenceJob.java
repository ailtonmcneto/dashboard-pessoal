package com.vida.financas;

import com.vida.auth.User;
import com.vida.auth.UserRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Gera os lançamentos das recorrências vencidas. Roda logo depois de subir (recupera o que passou
 * com a API desligada) e depois de hora em hora. "Hoje" é calculado no fuso de cada usuário.
 */
@Component
@ConditionalOnProperty(name = "vida.jobs.enabled", havingValue = "true", matchIfMissing = true)
class RecurrenceJob {

    private static final Logger log = LoggerFactory.getLogger(RecurrenceJob.class);

    private final RecurrenceRepository recurrences;
    private final RecurrenceService service;
    private final UserRepository users;
    private final TransactionTemplate tx;

    RecurrenceJob(RecurrenceRepository recurrences, RecurrenceService service, UserRepository users,
            TransactionTemplate tx) {
        this.recurrences = recurrences;
        this.service = service;
        this.users = users;
        this.tx = tx;
    }

    @Scheduled(initialDelayString = "PT30S", fixedDelayString = "PT1H")
    void run() {
        Integer generated = tx.execute(status -> generateAll());
        if (generated != null && generated > 0) {
            log.info("Recorrências: {} lançamento(s) gerado(s)", generated);
        }
    }

    int generateAll() {
        // UTC+14 é o fuso mais adiantado: ninguém tem "hoje" depois disso
        LocalDate latestToday = LocalDate.now(ZoneOffset.ofHours(14));
        var due = recurrences.findByActiveTrueAndNextOnLessThanEqual(latestToday);
        if (due.isEmpty()) {
            return 0;
        }
        Map<UUID, User> owners = users.findAllById(due.stream().map(Recurrence::getUserId).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        int total = 0;
        for (Recurrence r : due) {
            LocalDate today = LocalDate.now(ZoneId.of(owners.get(r.getUserId()).getTimezone()));
            total += service.generate(r, today);
        }
        return total;
    }
}
