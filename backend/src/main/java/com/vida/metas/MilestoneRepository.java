package com.vida.metas;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

interface MilestoneRepository extends Repository<Milestone, UUID> {

    Optional<Milestone> findByIdAndUserId(UUID id, UUID userId);

    List<Milestone> findByUserIdAndGoalIdInOrderByPositionAscCreatedAtAsc(UUID userId, Collection<UUID> goalIds);

    int countByGoalId(UUID goalId);

    Milestone save(Milestone milestone);

    void delete(Milestone milestone);
}
