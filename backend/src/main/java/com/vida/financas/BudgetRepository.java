package com.vida.financas;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

interface BudgetRepository extends Repository<Budget, UUID> {

    Optional<Budget> findByIdAndUserId(UUID id, UUID userId);

    Optional<Budget> findByUserIdAndCategoryIdAndMonth(UUID userId, UUID categoryId, LocalDate month);

    List<Budget> findByUserIdAndMonth(UUID userId, LocalDate month);

    Budget save(Budget budget);

    void delete(Budget budget);
}
