package com.vida.academico;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/** Toda busca leva o dono ({@code userId}); não há {@code findById} puro. */
interface ClassSlotRepository extends Repository<ClassSlot, UUID> {
    Optional<ClassSlot> findByIdAndUserId(UUID id, UUID userId);

    List<ClassSlot> findByUserIdAndCourseIdOrderByWeekdayAscStartTimeAsc(UUID userId, UUID courseId);

    List<ClassSlot> findByUserIdAndCourseIdInOrderByWeekdayAscStartTimeAsc(UUID userId, Collection<UUID> courseIds);

    ClassSlot save(ClassSlot slot);

    void delete(ClassSlot slot);
}
