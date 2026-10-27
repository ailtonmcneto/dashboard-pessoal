package com.vida.academico;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** Toda busca leva o dono ({@code userId}); não há {@code findById} puro. */
interface TopicRepository extends Repository<Topic, UUID> {
    Optional<Topic> findByIdAndUserId(UUID id, UUID userId);

    List<Topic> findByUserIdAndIdIn(UUID userId, Collection<UUID> ids);

    List<Topic> findByUserIdAndCourseIdOrderByTitle(UUID userId, UUID courseId);

    List<Topic> findByUserIdAndResourceIdOrderByTitle(UUID userId, UUID resourceId);

    List<Topic> findByUserIdOrderByTitle(UUID userId);

    /** Para revisar: com data vencida, ou nunca revisados. */
    @Query("select t from Topic t where t.userId = :userId "
            + "and (t.nextReviewOn is null or t.nextReviewOn <= :day) "
            + "order by t.nextReviewOn asc nulls first, t.title")
    List<Topic> findDue(UUID userId, LocalDate day);

    Topic save(Topic topic);

    void delete(Topic topic);
}
