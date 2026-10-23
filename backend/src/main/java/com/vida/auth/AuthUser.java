package com.vida.auth;

import java.io.Serial;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** Usuário autenticado guardado na sessão. O hash da senha é apagado após o login. */
public final class AuthUser implements UserDetails, CredentialsContainer {

    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID id;
    private final String email;
    private final String timezone;
    private String passwordHash;

    AuthUser(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.timezone = user.getTimezone();
        this.passwordHash = user.getPasswordHash();
    }

    public UUID getId() {
        return id;
    }

    /** Fuso do usuário; sessões gravadas antes deste campo existir caem no padrão. */
    public ZoneId getZone() {
        return ZoneId.of(timezone != null ? timezone : AuthService.DEFAULT_TIMEZONE);
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public void eraseCredentials() {
        passwordHash = null;
    }
}
