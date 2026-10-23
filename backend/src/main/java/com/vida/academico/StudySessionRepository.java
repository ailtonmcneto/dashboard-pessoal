package com.vida.academico;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/** Toda busca leva o dono ({@code userId}); não há {@code findById} puro. */
interface StudySessionRepository extends Repository<StudySession, UUID> {
    Optional<StudySession> findByIdAndUserId(UUID id, UUID userId);

    List<StudySession> findByUserIdAndStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtDesc(
            UUID userId, Instant from, Instant to);

    StudySession save(StudySession session);

    void delete(StudySession session);
}
