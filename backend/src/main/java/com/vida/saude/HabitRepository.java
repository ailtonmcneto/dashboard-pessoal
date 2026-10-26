package com.vida.saude;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

interface HabitRepository extends Repository<Habit, UUID> {

    Optional<Habit> findByIdAndUserId(UUID id, UUID userId);

    List<Habit> findByUserIdOrderByArchivedAscNameAsc(UUID userId);

    Habit save(Habit habit);

    void delete(Habit habit);
}
