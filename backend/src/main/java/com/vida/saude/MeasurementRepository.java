package com.vida.saude;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

interface MeasurementRepository extends Repository<Measurement, UUID> {

    Optional<Measurement> findByIdAndUserId(UUID id, UUID userId);

    List<Measurement> findByUserIdAndMetricTypeIdAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThanOrderByMeasuredAtDesc(
            UUID userId, UUID metricTypeId, Instant from, Instant to);

    List<Measurement> findByUserIdAndMetricTypeIdInAndMeasuredAtGreaterThanEqual(
            UUID userId, Collection<UUID> metricTypeIds, Instant from);

    Optional<Measurement> findFirstByUserIdAndMetricTypeIdOrderByMeasuredAtDesc(UUID userId, UUID metricTypeId);

    Measurement save(Measurement measurement);

    void delete(Measurement measurement);
}
