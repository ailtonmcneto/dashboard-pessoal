package com.vida.auth;

import com.vida.auth.AuthDtos.RegisterRequest;
import com.vida.common.BusinessException;
import com.vida.common.NotFoundException;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    static final String DEFAULT_TIMEZONE = "America/Sao_Paulo";

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final boolean signupAlwaysOpen;

    AuthService(UserRepository users, PasswordEncoder passwordEncoder,
            @Value("${vida.auth.signup-open:false}") boolean signupAlwaysOpen) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.signupAlwaysOpen = signupAlwaysOpen;
    }

    /** Uso pessoal: só o primeiro cadastro é aceito, a menos que {@code vida.auth.signup-open=true}. */
    @Transactional(readOnly = true)
    public boolean isSignupOpen() {
        return signupAlwaysOpen || users.count() == 0;
    }

    @Transactional
    public User register(RegisterRequest req) {
        if (!isSignupOpen()) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Cadastro fechado");
        }
        String email = normalizeEmail(req.email());
        if (users.existsByEmail(email)) {
            throw BusinessException.conflict("E-mail já cadastrado");
        }
        String timezone = req.timezone() == null || req.timezone().isBlank() ? DEFAULT_TIMEZONE : req.timezone();
        try {
            ZoneId.of(timezone);
        } catch (DateTimeException e) {
            throw BusinessException.badRequest("Fuso horário inválido: " + timezone);
        }
        return users.save(new User(email, req.name().strip(), passwordEncoder.encode(req.password()), timezone));
    }

    @Transactional(readOnly = true)
    public User get(UUID id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("Usuário"));
    }

    static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
