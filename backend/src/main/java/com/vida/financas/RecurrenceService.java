package com.vida.financas;

import com.vida.common.BusinessException;
import com.vida.common.CurrentUser;
import com.vida.common.NotFoundException;
import com.vida.financas.FinanceDtos.RecurrenceRequest;
import com.vida.financas.FinanceDtos.RecurrenceResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Recorrências e a geração dos lançamentos delas. */
@Service
public class RecurrenceService {

    /** Trava de segurança: nunca gera mais que isso de uma vez (ex.: semanal começando há anos). */
    static final int MAX_PER_RUN = 120;

    private final RecurrenceRepository recurrences;
    private final TransactionRepository transactions;
    private final CreditCardRepository cardRepository;
    private final AccountService accounts;
    private final CardService cards;
    private final CategoryService categories;
    private final CurrentUser currentUser;

    RecurrenceService(RecurrenceRepository recurrences, TransactionRepository transactions,
            CreditCardRepository cardRepository, AccountService accounts, CardService cards,
            CategoryService categories, CurrentUser currentUser) {
        this.recurrences = recurrences;
        this.transactions = transactions;
        this.cardRepository = cardRepository;
        this.accounts = accounts;
        this.cards = cards;
        this.categories = categories;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<RecurrenceResponse> list() {
        return recurrences.findByUserIdOrderByActiveDescNextOnAsc(currentUser.id()).stream()
                .map(RecurrenceService::toResponse)
                .toList();
    }

    /** Cria e já gera o que estiver vencido até hoje (inclusive ocorrências passadas). */
    @Transactional
    public RecurrenceResponse create(RecurrenceRequest req) {
        Recurrence r = new Recurrence(currentUser.id());
        apply(r, req);
        recurrences.save(r);
        generate(r, currentUser.today());
        return toResponse(r);
    }

    @Transactional
    public RecurrenceResponse update(UUID id, RecurrenceRequest req) {
        Recurrence r = find(id);
        apply(r, req);
        generate(r, currentUser.today());
        return toResponse(r);
    }

    @Transactional
    public RecurrenceResponse setActive(UUID id, boolean active) {
        Recurrence r = find(id);
        if (active && r.getEndsOn() != null && r.getNextOn().isAfter(r.getEndsOn())) {
            throw BusinessException.badRequest("A recorrência já terminou");
        }
        r.setActive(active);
        if (active) {
            generate(r, currentUser.today());
        }
        return toResponse(r);
    }

    /** Apaga o molde; os lançamentos já gerados continuam (sem vínculo). */
    @Transactional
    public void delete(UUID id) {
        recurrences.delete(find(id));
    }

    /**
     * Gera os lançamentos de todas as ocorrências até {@code today}. Não usa o usuário logado:
     * também roda no job, para qualquer usuário (o dono vem da própria recorrência).
     */
    int generate(Recurrence r, LocalDate today) {
        CreditCard card = r.getCardId() == null ? null
                : cardRepository.findByIdAndUserId(r.getCardId(), r.getUserId()).orElse(null);
        int count = 0;
        while (r.isActive() && !r.getNextOn().isAfter(today) && count < MAX_PER_RUN) {
            LocalDate date = r.getNextOn();
            Transaction t = new Transaction(r.getUserId(), r.getKind());
            // em conta, nasce pendente: o usuário confirma quando pagar/receber de fato
            boolean onCard = card != null;
            t.setDetails(date, r.getDescription(), r.getAmount(), r.getCategoryId(), null, onCard);
            t.setPlace(r.getAccountId(), onCard ? card.getId() : null, null,
                    onCard ? Schedules.invoiceFor(date, card.getClosingDay()).atDay(1) : null);
            t.setRecurrenceId(r.getId());
            transactions.save(t);
            r.advance();
            count++;
        }
        return count;
    }

    private Recurrence find(UUID id) {
        return recurrences.findByIdAndUserId(id, currentUser.id())
                .orElseThrow(() -> new NotFoundException("Recorrência"));
    }

    private void apply(Recurrence r, RecurrenceRequest req) {
        if (req.kind() != Transaction.Kind.RECEITA && req.kind() != Transaction.Kind.DESPESA) {
            throw BusinessException.badRequest("Recorrência é de receita ou despesa");
        }
        if ((req.accountId() == null) == (req.cardId() == null)) {
            throw BusinessException.badRequest("Escolha uma conta ou um cartão (um dos dois)");
        }
        if (req.cardId() != null && req.kind() != Transaction.Kind.DESPESA) {
            throw BusinessException.badRequest("No cartão só entram despesas");
        }
        if (req.endsOn() != null && req.endsOn().isBefore(req.startsOn())) {
            throw BusinessException.badRequest("O fim não pode ser antes do início");
        }
        if (req.accountId() != null) {
            accounts.findUsable(req.accountId());
        }
        if (req.cardId() != null) {
            cards.findUsable(req.cardId());
        }
        if (req.categoryId() != null
                && !categories.find(req.categoryId()).getKind().name().equals(req.kind().name())) {
            throw BusinessException.badRequest("A categoria não é do mesmo tipo da recorrência");
        }
        r.update(req.description().strip(), req.amount(), req.kind(), req.categoryId(), req.accountId(),
                req.cardId(), req.frequency() == null ? Recurrence.Frequency.MENSAL : req.frequency(),
                req.startsOn(), req.endsOn(), currentUser.today());
    }

    static RecurrenceResponse toResponse(Recurrence r) {
        return new RecurrenceResponse(r.getId(), r.getDescription(), r.getAmount(), r.getKind(), r.getCategoryId(),
                r.getAccountId(), r.getCardId(), r.getFrequency(), r.getStartsOn(), r.getEndsOn(), r.getNextOn(),
                r.isActive(), r.getGenerated());
    }
}
