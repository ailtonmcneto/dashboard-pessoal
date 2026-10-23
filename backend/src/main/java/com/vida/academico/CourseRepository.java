package com.vida.academico;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/** Toda busca leva o dono ({@code userId}); não há {@code findById} puro. */
interface CourseRepository extends Repository<Course, UUID> {
    Optional<Course> findByIdAndUserId(UUID id, UUID userId);

    List<Course> findByUserIdAndTermIdOrderByName(UUID userId, UUID termId);

    List<Course> findByUserIdAndIdIn(UUID userId, Collection<UUID> ids);

    List<Course> findByUserIdOrderByName(UUID userId);

    boolean existsByTermIdAndNameIgnoreCaseAndIdNot(UUID termId, String name, UUID id);

    boolean existsByTermIdAndNameIgnoreCase(UUID termId, String name);

    Course save(Course course);

    void delete(Course course);
}
