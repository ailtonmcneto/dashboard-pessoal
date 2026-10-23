package com.vida.common;

import com.vida.auth.AuthUser;
import java.util.UUID;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Id do usuário logado — a única fonte de {@code user_id} para os services. */
@Component
public class CurrentUser {

    public UUID id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthUser user) {
            return user.getId();
        }
        throw new AuthenticationCredentialsNotFoundException("Sem usuário autenticado");
    }
}
