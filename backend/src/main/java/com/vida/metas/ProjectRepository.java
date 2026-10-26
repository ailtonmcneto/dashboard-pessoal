package com.vida.metas;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

interface ProjectRepository extends Repository<Project, UUID> {

    Optional<Project> findByIdAndUserId(UUID id, UUID userId);

    List<Project> findByUserIdOrderByStatusAscDueOnAscTitleAsc(UUID userId);

    List<Project> findByUserIdAndIdIn(UUID userId, Collection<UUID> ids);

    Project save(Project project);

    void delete(Project project);
}
