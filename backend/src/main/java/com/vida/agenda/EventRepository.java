package com.vida.agenda;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/** Sem {@code findById} puro: toda busca leva o dono. */
interface EventRepository extends Repository<Event, UUID> {

    Optional<Event> findByIdAndUserId(UUID id, UUID userId);

    List<Event> findByUserIdAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAt(
            UUID userId, Instant from, Instant to);

    Event save(Event event);

    void delete(Event event);
}
