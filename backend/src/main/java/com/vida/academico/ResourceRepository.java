package com.vida.academico;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/** Toda busca leva o dono ({@code userId}); não há {@code findById} puro. */
interface ResourceRepository extends Repository<Resource, UUID> {
    Optional<Resource> findByIdAndUserId(UUID id, UUID userId);

    List<Resource> findByUserIdOrderByStatusAscTitleAsc(UUID userId);

    List<Resource> findByUserIdAndIdIn(UUID userId, Collection<UUID> ids);

    Resource save(Resource resource);

    void delete(Resource resource);
}
