package com.vida.saude;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

interface MetricTypeRepository extends Repository<MetricType, UUID> {

    Optional<MetricType> findByIdAndUserId(UUID id, UUID userId);

    List<MetricType> findByUserIdOrderByArchivedAscNameAsc(UUID userId);

    boolean existsByUserId(UUID userId);

    MetricType save(MetricType metric);

    void delete(MetricType metric);
}
