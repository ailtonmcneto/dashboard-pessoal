package com.vida.financas;

import com.vida.common.BusinessException;
import com.vida.common.CurrentUser;
import com.vida.common.NotFoundException;
import com.vida.common.Strings;
import com.vida.financas.FinanceDtos.TransactionRequest;
import com.vida.financas.FinanceDtos.TransactionResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    static final int MAX_RANGE_DAYS = 366;

    private final TransactionRepository transactions;
    private final AccountService accounts;
    private final CardService cards;
    private final CategoryService categories;
    private final CurrentUser currentUser;

    TransactionService(TransactionRepository transactions, AccountService accounts, CardService cards,
            CategoryService categories, CurrentUser currentUser) {
        this.transactions = transactions;
        this.accounts = accounts;
        this.cards = cards;
        this.categories = categories;
        this.currentUser = currentUser;
    }

    /**
     * Lançamentos entre {@code from} e {@code to} (inclusive). Filtros opcionais: conta (origem ou destino),
     * cartão e categoria (inclui as subcategorias).
     */
    @Transactional(readOnly = true)
    public List<TransactionResponse> list(LocalDate from, LocalDate to, @Nullable UUID accountId,
            @Nullable UUID cardId, @Nullable UUID categoryId) {
        if (to.isBefore(from)) {
            throw BusinessException.badRequest("'to' não pode ser antes de 'from'");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw BusinessException.badRequest("Intervalo máximo de 366 dias");
        }
        Set<UUID> categoryIds = categoryId == null ? null : withChildren(categoryId);
        return transactions.findByUserIdAndDateBetweenOrderByDateDescCreatedAtDesc(currentUser.id(), from, to)
                .stream()
                .filter(t -> accountId == null || accountId.equals(t.getAccountId())
                        || accountId.equals(t.getToAccountId()))
                .filter(t -> cardId == null || cardId.equals(t.getCardId()))
                .filter(t -> categoryIds == null || categoryIds.contains(t.getCategoryId()))
                .map(TransactionService::toResponse)
                .toList();
    }

    /** Cria o lançamento; compra parcelada no cartão vira um lançamento por parcela. */
    @Transactional
    public List<TransactionResponse> create(TransactionRequest req) {
        int installments = req.installments() == null ? 1 : req.installments();
        if (req.kind() == Transaction.Kind.PAGAMENTO_FATURA) {
            throw BusinessException.badRequest("Pagamento de fatura é feito pela fatura do cartão");
        }
        if (installments > 1 && (req.kind() != Transaction.Kind.DESPESA || req.cardId() == null)) {
            throw BusinessException.badRequest("Parcelamento só vale para compras no cartão");
        }

        UUID userId = currentUser.id();
        if (installments == 1) {
            Transaction t = new Transaction(userId, req.kind());
            apply(t, req, req.date(), req.amount());
            return List.of(toResponse(transactions.save(t)));
        }

        UUID group = UUID.randomUUID();
        List<BigDecimal> parts = Schedules.split(req.amount(), installments);
        List<TransactionResponse> created = new ArrayList<>();
        for (int i = 0; i < installments; i++) {
            Transaction t = new Transaction(userId, req.kind());
            apply(t, req, req.date().plusMonths(i), parts.get(i));
            t.setInstallment(group, i + 1, installments);
            created.add(toResponse(transactions.save(t)));
        }
        return created;
    }

    /** Edita um lançamento (numa compra parcelada, só a parcela escolhida). O tipo não muda. */
    @Transactional
    public TransactionResponse update(UUID id, TransactionRequest req) {
        Transaction t = find(id);
        if (t.getKind() != req.kind()) {
            throw BusinessException.badRequest("Não dá para trocar o tipo do lançamento; apague e crie outro");
        }
        if (t.getKind() == Transaction.Kind.PAGAMENTO_FATURA) {
            Account account = accounts.findUsable(require(req.accountId(), "conta"));
            t.setDetails(req.date(), req.description().strip(), req.amount(), null, Strings.clean(req.notes()), true);
            t.setPlace(account.getId(), t.getCardId(), null, t.getInvoiceMonth());
            return toResponse(t);
        }
        apply(t, req, req.date(), req.amount());
        return toResponse(t);
    }

    /** Apaga o lançamento; com {@code allInstallments}, todas as parcelas da mesma compra. */
    @Transactional
    public void delete(UUID id, boolean allInstallments) {
        Transaction t = find(id);
        if (allInstallments && t.getInstallmentGroupId() != null) {
            transactions.deleteAll(transactions.findByUserIdAndInstallmentGroupIdOrderByInstallmentNo(
                    currentUser.id(), t.getInstallmentGroupId()));
        } else {
            transactions.delete(t);
        }
    }

    private Transaction find(UUID id) {
        return transactions.findByIdAndUserId(id, currentUser.id())
                .orElseThrow(() -> new NotFoundException("Lançamento"));
    }

    private void apply(Transaction t, TransactionRequest req, LocalDate date, BigDecimal amount) {
        UUID categoryId = req.categoryId();
        UUID accountId = null;
        UUID cardId = null;
        UUID toAccountId = null;
        LocalDate invoiceMonth = null;
        boolean paid = req.paid() == null || req.paid();

        switch (req.kind()) {
            case RECEITA -> {
                if (req.cardId() != null || req.toAccountId() != null) {
                    throw BusinessException.badRequest("Receita entra numa conta, não num cartão");
                }
                accountId = accounts.findUsable(require(req.accountId(), "conta")).getId();
            }
            case DESPESA -> {
                if ((req.accountId() == null) == (req.cardId() == null)) {
                    throw BusinessException.badRequest("Despesa sai de uma conta ou de um cartão (um dos dois)");
                }
                if (req.toAccountId() != null) {
                    throw BusinessException.badRequest("Despesa não tem conta de destino");
                }
                if (req.accountId() != null) {
                    accountId = accounts.findUsable(req.accountId()).getId();
                } else {
                    CreditCard card = cards.findUsable(req.cardId());
                    cardId = card.getId();
                    invoiceMonth = Schedules.invoiceFor(date, card.getClosingDay()).atDay(1);
                    paid = true; // no cartão, "pagar" é pagar a fatura
                }
            }
            case TRANSFERENCIA -> {
                if (req.cardId() != null || categoryId != null) {
                    throw BusinessException.badRequest("Transferência é só entre contas, sem categoria");
                }
                accountId = accounts.findUsable(require(req.accountId(), "conta de origem")).getId();
                toAccountId = accounts.findUsable(require(req.toAccountId(), "conta de destino")).getId();
                if (accountId.equals(toAccountId)) {
                    throw BusinessException.badRequest("Origem e destino precisam ser contas diferentes");
                }
                paid = true;
            }
            case PAGAMENTO_FATURA -> throw new IllegalStateException();
        }

        if (categoryId != null) {
            Category category = categories.find(categoryId);
            if (!category.getKind().name().equals(req.kind().name())) {
                throw BusinessException.badRequest("A categoria " + category.getName() + " é de "
                        + category.getKind().name().toLowerCase() + ", não de " + req.kind().name().toLowerCase());
            }
        }

        t.setDetails(date, req.description().strip(), amount, categoryId, Strings.clean(req.notes()), paid);
        t.setPlace(accountId, cardId, toAccountId, invoiceMonth);
    }

    private static UUID require(@Nullable UUID id, String what) {
        if (id == null) {
            throw BusinessException.badRequest("Informe a " + what);
        }
        return id;
    }

    private Set<UUID> withChildren(UUID categoryId) {
        categories.find(categoryId);
        Set<UUID> ids = new HashSet<>();
        ids.add(categoryId);
        categories.all().stream()
                .filter(c -> categoryId.equals(c.getParentId()))
                .forEach(c -> ids.add(c.getId()));
        return ids;
    }

    static TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(t.getId(), t.getKind(), t.getDate(), t.getDescription(), t.getAmount(),
                t.getCategoryId(), t.getAccountId(), t.getCardId(), t.getToAccountId(),
                t.getInvoiceMonth() == null ? null : YearMonth.from(t.getInvoiceMonth()), t.isPaid(),
                t.getInstallmentGroupId(),
                t.getInstallmentNo() == null ? null : t.getInstallmentNo().intValue(),
                t.getInstallmentTotal() == null ? null : t.getInstallmentTotal().intValue(),
                t.getRecurrenceId(), t.getNotes());
    }
}
