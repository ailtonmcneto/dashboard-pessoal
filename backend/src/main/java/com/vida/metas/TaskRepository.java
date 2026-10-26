package com.vida.metas;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

interface TaskRepository extends Repository<Task, UUID> {

    Optional<Task> findByIdAndUserId(UUID id, UUID userId);

    List<Task> findByUserIdAndDoneAtIsNull(UUID userId);

    List<Task> findByUserIdAndDoneAtGreaterThanEqualOrderByDoneAtDesc(UUID userId, Instant since);

    /** Todas as tarefas (abertas e feitas) — usado nos contadores de progresso. */
    List<Task> findByUserId(UUID userId);

    Task save(Task task);

    void delete(Task task);
}
