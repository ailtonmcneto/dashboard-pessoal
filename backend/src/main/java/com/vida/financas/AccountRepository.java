package com.vida.financas;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

interface AccountRepository extends Repository<Account, UUID> {

    Optional<Account> findByIdAndUserId(UUID id, UUID userId);

    List<Account> findByUserIdOrderByArchivedAscNameAsc(UUID userId);

    List<Account> findByUserIdAndIdIn(UUID userId, Collection<UUID> ids);

    boolean existsByUserIdAndNameIgnoreCase(UUID userId, String name);

    boolean existsByUserIdAndNameIgnoreCaseAndIdNot(UUID userId, String name, UUID id);

    Account save(Account account);

    void delete(Account account);
}
