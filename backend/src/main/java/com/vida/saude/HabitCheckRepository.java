package com.vida.saude;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

interface HabitCheckRepository extends Repository<HabitCheck, UUID> {

    Optional<HabitCheck> findByUserIdAndHabitIdAndDate(UUID userId, UUID habitId, LocalDate date);

    List<HabitCheck> findByUserIdAndHabitIdInAndDateGreaterThanEqual(UUID userId, Collection<UUID> habitIds,
            LocalDate from);

    List<HabitCheck> findByUserIdAndHabitIdAndDateBetweenOrderByDate(UUID userId, UUID habitId, LocalDate from,
            LocalDate to);

    HabitCheck save(HabitCheck check);

    void delete(HabitCheck check);
}
