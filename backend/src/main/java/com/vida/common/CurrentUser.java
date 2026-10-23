package com.vida.common;

import com.vida.auth.AuthUser;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Usuário logado — a única fonte de {@code user_id} (e do fuso) para os services. */
@Component
public class CurrentUser {

    public UUID id() {
        return principal().getId();
    }

    /** "Hoje" no fuso do usuário — é o que vale para prazos, revisões e faltas. */
    public LocalDate today() {
        return LocalDate.now(principal().getZone());
    }

    public ZoneId zone() {
        return principal().getZone();
    }

    private static AuthUser principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthUser user) {
            return user;
        }
        throw new AuthenticationCredentialsNotFoundException("Sem usuário autenticado");
    }
}
