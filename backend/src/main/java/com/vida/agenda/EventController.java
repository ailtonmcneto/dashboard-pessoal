package com.vida.agenda;

import com.vida.agenda.EventDtos.EventRequest;
import com.vida.agenda.EventDtos.EventResponse;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/events")
class EventController {

    private final EventService service;

    EventController(EventService service) {
        this.service = service;
    }

    /** Sem parâmetros: de agora até 30 dias à frente. */
    @GetMapping
    List<EventResponse> list(@RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        Instant start = from != null ? from : Instant.now();
        Instant end = to != null ? to : start.plus(30, ChronoUnit.DAYS);
        return service.list(start, end).stream().map(EventResponse::of).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    EventResponse create(@Valid @RequestBody EventRequest req) {
        return EventResponse.of(service.create(req));
    }

    @PutMapping("/{id}")
    EventResponse update(@PathVariable UUID id, @Valid @RequestBody EventRequest req) {
        return EventResponse.of(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
