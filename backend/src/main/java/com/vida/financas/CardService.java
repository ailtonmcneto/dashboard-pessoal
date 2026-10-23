package com.vida.financas;

import com.vida.common.BusinessException;
import com.vida.common.CurrentUser;
import com.vida.common.NotFoundException;
import com.vida.common.Strings;
import com.vida.financas.FinanceDtos.CardRequest;
import com.vida.financas.FinanceDtos.CardResponse;
import com.vida.financas.FinanceDtos.InvoiceResponse;
import com.vida.financas.FinanceDtos.InvoiceStatus;
import com.vida.financas.FinanceDtos.InvoiceSummary;
import com.vida.financas.FinanceDtos.PayInvoiceRequest;
import com.vida.financas.FinanceDtos.TransactionResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardService {

    private final CreditCardRepository cards;
    private final TransactionRepository transactions;
    private final AccountService accounts;
    private final FinanceQueries queries;
    private final CurrentUser currentUser;

    CardService(CreditCardRepository cards, TransactionRepository transactions, AccountService accounts,
            FinanceQueries queries, CurrentUser currentUser) {
        this.cards = cards;
        this.transactions = transactions;
        this.accounts = accounts;
        this.queries = queries;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<CardResponse> list() {
        Map<UUID, BigDecimal> outstanding = queries.cardOutstanding(currentUser.id());
        return cards.findByUserIdOrderByArchivedAscNameAsc(currentUser.id()).stream()
                .map(c -> toResponse(c, outstanding.getOrDefault(c.getId(), BigDecimal.ZERO)))
                .toList();
    }

    @Transactional
    public CardResponse create(CardRequest req) {
        if (cards.existsByUserIdAndNameIgnoreCase(currentUser.id(), req.name().strip())) {
            throw BusinessException.conflict("Já existe um cartão com esse nome");
        }
        CreditCard card = new CreditCard(currentUser.id());
        apply(card, req);
        return toResponse(cards.save(card), BigDecimal.ZERO);
    }

    @Transactional
    public CardResponse update(UUID id, CardRequest req) {
        CreditCard card = find(id);
        if (cards.existsByUserIdAndNameIgnoreCaseAndIdNot(currentUser.id(), req.name().strip(), id)) {
            throw BusinessException.conflict("Já existe um cartão com esse nome");
        }
        apply(card, req);
        return toResponse(card, queries.cardOutstanding(currentUser.id()).getOrDefault(id, BigDecimal.ZERO));
    }

    @Transactional
    public void delete(UUID id) {
        CreditCard card = find(id);
        if (transactions.existsByCardId(id)) {
            throw BusinessException.conflict("O cartão tem lançamentos; arquive-o em vez de apagar");
        }
        cards.delete(card);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse invoice(UUID cardId, YearMonth month) {
        CreditCard card = find(cardId);
        List<TransactionResponse> items = transactions
                .findByUserIdAndCardIdAndInvoiceMonthOrderByDateAscCreatedAtAsc(currentUser.id(), cardId, month.atDay(1))
                .stream().map(TransactionService::toResponse).toList();
        return new InvoiceResponse(cardId, summary(card, month), items);
    }

    /** Paga a fatura (por padrão, o que falta) com dinheiro de uma conta. */
    @Transactional
    public InvoiceResponse pay(UUID cardId, YearMonth month, PayInvoiceRequest req) {
        CreditCard card = find(cardId);
        Account account = accounts.findUsable(req.accountId());
        InvoiceSummary summary = summary(card, month);
        BigDecimal amount = req.amount() != null ? req.amount() : summary.remaining();
        if (amount.signum() <= 0) {
            throw BusinessException.badRequest("Não há nada a pagar nessa fatura");
        }
        Transaction t = new Transaction(currentUser.id(), Transaction.Kind.PAGAMENTO_FATURA);
        t.setDetails(req.date() != null ? req.date() : currentUser.today(),
                "Fatura " + card.getName() + " " + month, amount, null, null, true);
        t.setPlace(account.getId(), card.getId(), null, month.atDay(1));
        transactions.save(t);
        return invoice(cardId, month);
    }

    CreditCard find(UUID id) {
        return cards.findByIdAndUserId(id, currentUser.id()).orElseThrow(() -> new NotFoundException("Cartão"));
    }

    CreditCard findUsable(UUID id) {
        CreditCard card = find(id);
        if (card.isArchived()) {
            throw BusinessException.badRequest("O cartão " + card.getName() + " está arquivado");
        }
        return card;
    }

    InvoiceSummary summary(CreditCard card, YearMonth month) {
        FinanceQueries.InvoiceTotals totals = queries.invoice(currentUser.id(), card.getId(), month);
        LocalDate closing = Schedules.closingDate(month, card.getClosingDay());
        LocalDate due = Schedules.dueDate(month, card.getClosingDay(), card.getDueDay());
        BigDecimal remaining = totals.purchases().subtract(totals.payments()).max(BigDecimal.ZERO);
        LocalDate today = currentUser.today();
        InvoiceStatus status;
        if (totals.purchases().signum() > 0 && remaining.signum() == 0) {
            status = InvoiceStatus.PAGA;
        } else if (today.isBefore(closing)) {
            status = InvoiceStatus.ABERTA;
        } else if (today.isAfter(due) && remaining.signum() > 0) {
            status = InvoiceStatus.ATRASADA;
        } else if (remaining.signum() == 0) {
            status = InvoiceStatus.PAGA;
        } else {
            status = InvoiceStatus.FECHADA;
        }
        return new InvoiceSummary(month, closing, due, totals.purchases(), totals.payments(), remaining, status);
    }

    private void apply(CreditCard card, CardRequest req) {
        UUID paymentAccountId = req.paymentAccountId();
        if (paymentAccountId != null) {
            accounts.find(paymentAccountId);
        }
        card.update(Strings.clean(req.name()), req.creditLimit(), req.closingDay(), req.dueDay(), paymentAccountId,
                Boolean.TRUE.equals(req.archived()));
    }

    private CardResponse toResponse(CreditCard c, BigDecimal used) {
        BigDecimal available = c.getCreditLimit() == null ? null : c.getCreditLimit().subtract(used);
        YearMonth current = Schedules.invoiceFor(currentUser.today(), c.getClosingDay());
        return new CardResponse(c.getId(), c.getName(), c.getCreditLimit(), c.getClosingDay(), c.getDueDay(),
                c.getPaymentAccountId(), c.isArchived(), used, available, summary(c, current));
    }
}
