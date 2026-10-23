package com.vida.financas;

import com.vida.common.BusinessException;
import com.vida.common.CurrentUser;
import com.vida.common.NotFoundException;
import com.vida.financas.FinanceDtos.AccountRequest;
import com.vida.financas.FinanceDtos.AccountResponse;
import com.vida.financas.FinanceQueries.Balance;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final FinanceQueries queries;
    private final CurrentUser currentUser;

    AccountService(AccountRepository accounts, TransactionRepository transactions, FinanceQueries queries,
            CurrentUser currentUser) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.queries = queries;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> list() {
        Map<UUID, Balance> balances = queries.accountBalances(currentUser.id());
        return accounts.findByUserIdOrderByArchivedAscNameAsc(currentUser.id()).stream()
                .map(a -> toResponse(a, balances.get(a.getId())))
                .toList();
    }

    @Transactional
    public AccountResponse create(AccountRequest req) {
        if (accounts.existsByUserIdAndNameIgnoreCase(currentUser.id(), req.name().strip())) {
            throw BusinessException.conflict("Já existe uma conta com esse nome");
        }
        Account account = new Account(currentUser.id());
        apply(account, req);
        accounts.save(account);
        return toResponse(account, queries.accountBalances(currentUser.id()).get(account.getId()));
    }

    @Transactional
    public AccountResponse update(UUID id, AccountRequest req) {
        Account account = find(id);
        if (accounts.existsByUserIdAndNameIgnoreCaseAndIdNot(currentUser.id(), req.name().strip(), id)) {
            throw BusinessException.conflict("Já existe uma conta com esse nome");
        }
        apply(account, req);
        return toResponse(account, queries.accountBalances(currentUser.id()).get(id));
    }

    /** Só apaga conta sem lançamentos; com histórico, o caminho é arquivar. */
    @Transactional
    public void delete(UUID id) {
        Account account = find(id);
        if (transactions.existsByAccountIdOrToAccountId(id, id)) {
            throw BusinessException.conflict("A conta tem lançamentos; arquive-a em vez de apagar");
        }
        accounts.delete(account);
    }

    Account find(UUID id) {
        return accounts.findByIdAndUserId(id, currentUser.id()).orElseThrow(() -> new NotFoundException("Conta"));
    }

    /** Para lançamentos novos: precisa existir, ser do usuário e não estar arquivada. */
    Account findUsable(UUID id) {
        Account account = find(id);
        if (account.isArchived()) {
            throw BusinessException.badRequest("A conta " + account.getName() + " está arquivada");
        }
        return account;
    }

    private static void apply(Account account, AccountRequest req) {
        account.update(req.name().strip(), req.kind() == null ? Account.Kind.CORRENTE : req.kind(),
                req.openingBalance() == null ? BigDecimal.ZERO : req.openingBalance(),
                Boolean.TRUE.equals(req.archived()));
    }

    private static AccountResponse toResponse(Account a, Balance balance) {
        BigDecimal paid = balance == null ? a.getOpeningBalance() : balance.paid();
        BigDecimal projected = balance == null ? a.getOpeningBalance() : balance.projected();
        return new AccountResponse(a.getId(), a.getName(), a.getKind(), a.getOpeningBalance(), paid, projected,
                a.isArchived());
    }
}
