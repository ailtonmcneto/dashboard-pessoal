package com.vida.academico;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/** Toda busca leva o dono ({@code userId}); não há {@code findById} puro. */
interface TermRepository extends Repository<Term, UUID> {
    Optional<Term> findByIdAndUserId(UUID id, UUID userId);

    List<Term> findByUserIdOrderByStartsOnDesc(UUID userId);

    boolean existsByUserIdAndNameIgnoreCaseAndIdNot(UUID userId, String name, UUID id);

    boolean existsByUserIdAndNameIgnoreCase(UUID userId, String name);

    Term save(Term term);

    void delete(Term term);
}
