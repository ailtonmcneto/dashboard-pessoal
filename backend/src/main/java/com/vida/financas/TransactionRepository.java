package com.vida.financas;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

interface TransactionRepository extends Repository<Transaction, UUID> {

    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    List<Transaction> findByUserIdAndInstallmentGroupIdOrderByInstallmentNo(UUID userId, UUID groupId);

    List<Transaction> findByUserIdAndCardIdAndInvoiceMonthOrderByDateAscCreatedAtAsc(
            UUID userId, UUID cardId, LocalDate invoiceMonth);

    List<Transaction> findByUserIdAndDateBetweenOrderByDateDescCreatedAtDesc(UUID userId, LocalDate from, LocalDate to);

    boolean existsByAccountIdOrToAccountId(UUID accountId, UUID toAccountId);

    boolean existsByCardId(UUID cardId);

    Transaction save(Transaction transaction);

    void delete(Transaction transaction);

    void deleteAll(Iterable<Transaction> transactions);
}
