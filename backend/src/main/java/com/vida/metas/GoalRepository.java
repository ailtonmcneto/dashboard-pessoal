package com.vida.metas;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

interface GoalRepository extends Repository<Goal, UUID> {

    Optional<Goal> findByIdAndUserId(UUID id, UUID userId);

    List<Goal> findByUserIdOrderByStatusAscDueOnAscTitleAsc(UUID userId);

    List<Goal> findByUserIdAndIdIn(UUID userId, Collection<UUID> ids);

    Goal save(Goal goal);

    void delete(Goal goal);
}
