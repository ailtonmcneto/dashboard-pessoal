package com.vida.agenda;

import com.vida.agenda.Event.Kind;
import com.vida.agenda.EventDtos.EventRequest;
import com.vida.common.BusinessException;
import com.vida.common.CurrentUser;
import com.vida.common.NotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {

    static final Duration MAX_RANGE = Duration.ofDays(366);

    private final EventRepository events;
    private final CurrentUser currentUser;

    EventService(EventRepository events, CurrentUser currentUser) {
        this.events = events;
        this.currentUser = currentUser;
    }

    /** Eventos que começam em [from, to). */
    @Transactional(readOnly = true)
    public List<Event> list(Instant from, Instant to) {
        if (!to.isAfter(from)) {
            throw BusinessException.badRequest("'to' precisa ser depois de 'from'");
        }
        if (Duration.between(from, to).compareTo(MAX_RANGE) > 0) {
            throw BusinessException.badRequest("Intervalo máximo de 366 dias");
        }
        return events.findByUserIdAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAt(
                currentUser.id(), from, to);
    }

    @Transactional
    public Event create(EventRequest req) {
        Event event = new Event(currentUser.id());
        apply(event, req);
        return events.save(event);
    }

    @Transactional
    public Event update(UUID id, EventRequest req) {
        Event event = find(id);
        apply(event, req);
        return event;
    }

    @Transactional
    public void delete(UUID id) {
        events.delete(find(id));
    }

    private Event find(UUID id) {
        return events.findByIdAndUserId(id, currentUser.id()).orElseThrow(() -> new NotFoundException("Evento"));
    }

    private static void apply(Event event, EventRequest req) {
        if (req.endsAt() != null && req.endsAt().isBefore(req.startsAt())) {
            throw BusinessException.badRequest("O fim não pode ser antes do início");
        }
        event.update(req.title().strip(), req.kind() == null ? Kind.COMPROMISSO : req.kind(),
                req.startsAt(), req.endsAt(), blankToNull(req.location()), blankToNull(req.notes()));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
