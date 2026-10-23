package com.vida.financas;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

interface RecurrenceRepository extends Repository<Recurrence, UUID> {

    Optional<Recurrence> findByIdAndUserId(UUID id, UUID userId);

    List<Recurrence> findByUserIdOrderByActiveDescNextOnAsc(UUID userId);

    List<Recurrence> findByUserIdAndActiveTrueAndNextOnLessThanEqual(UUID userId, LocalDate day);

    /** Para o job: todas as ativas com alguma ocorrência até {@code day} (o filtro fino é por fuso). */
    List<Recurrence> findByActiveTrueAndNextOnLessThanEqual(LocalDate day);

    Recurrence save(Recurrence recurrence);

    void delete(Recurrence recurrence);
}
