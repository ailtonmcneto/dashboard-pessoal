package com.vida.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 72, message = "a senha precisa ter entre 8 e 72 caracteres") String password,
            @Size(max = 64) @Nullable String timezone) {
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    public record MeResponse(UUID id, String name, String email, String timezone) {

        static MeResponse of(User user) {
            return new MeResponse(user.getId(), user.getName(), user.getEmail(), user.getTimezone());
        }
    }

    /** O front usa para decidir entre mostrar "Entrar" ou "Criar conta". */
    public record StatusResponse(boolean signupOpen) {
    }
}
