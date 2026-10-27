package com.vida.academico;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/** Toda busca leva o dono ({@code userId}); não há {@code findById} puro. */
interface AssessmentRepository extends Repository<Assessment, UUID> {
    Optional<Assessment> findByIdAndUserId(UUID id, UUID userId);

    List<Assessment> findByUserIdAndCourseIdOrderByDueOnAscTitleAsc(UUID userId, UUID courseId);

    List<Assessment> findByUserIdAndCourseIdIn(UUID userId, Collection<UUID> courseIds);

    List<Assessment> findByUserIdAndDueOnBetweenOrderByDueOn(UUID userId, LocalDate from, LocalDate to);

    Assessment save(Assessment assessment);

    void delete(Assessment assessment);
}
