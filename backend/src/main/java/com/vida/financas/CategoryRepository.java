package com.vida.financas;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

interface CategoryRepository extends Repository<Category, UUID> {

    Optional<Category> findByIdAndUserId(UUID id, UUID userId);

    List<Category> findByUserIdOrderByKindAscNameAsc(UUID userId);

    boolean existsByUserId(UUID userId);

    boolean existsByParentId(UUID parentId);

    Category save(Category category);

    void delete(Category category);
}
