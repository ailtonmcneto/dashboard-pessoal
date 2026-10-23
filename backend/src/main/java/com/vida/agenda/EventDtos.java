package com.vida.agenda;

import com.vida.agenda.Event.Kind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public final class EventDtos {

    private EventDtos() {
    }

    public record EventRequest(
            @NotBlank @Size(max = 200) String title,
            @Nullable Kind kind,
            @NotNull Instant startsAt,
            @Nullable Instant endsAt,
            @Size(max = 200) @Nullable String location,
            @Size(max = 2000) @Nullable String notes) {
    }

    public record EventResponse(
            UUID id, String title, Kind kind, Instant startsAt, @Nullable Instant endsAt,
            @Nullable String location, @Nullable String notes) {

        static EventResponse of(Event e) {
            return new EventResponse(e.getId(), e.getTitle(), e.getKind(), e.getStartsAt(), e.getEndsAt(),
                    e.getLocation(), e.getNotes());
        }
    }
}
