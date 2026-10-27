package com.vida.academico;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** Toda busca leva o dono ({@code userId}); não há {@code findById} puro. */
interface AbsenceRepository extends Repository<Absence, UUID> {
    Optional<Absence> findByIdAndUserId(UUID id, UUID userId);

    List<Absence> findByUserIdAndCourseIdOrderByDateDesc(UUID userId, UUID courseId);

    @Query("select a.courseId, sum(a.classesMissed) from Absence a "
            + "where a.userId = :userId and a.courseId in :courseIds group by a.courseId")
    List<Object[]> sumByCourse(UUID userId, Collection<UUID> courseIds);

    Absence save(Absence absence);

    void delete(Absence absence);
}
