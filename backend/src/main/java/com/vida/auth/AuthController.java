package com.vida.auth;

import com.vida.auth.AuthDtos.LoginRequest;
import com.vida.auth.AuthDtos.MeResponse;
import com.vida.auth.AuthDtos.RegisterRequest;
import com.vida.auth.AuthDtos.StatusResponse;
import com.vida.common.CurrentUser;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Logout fica no filtro do Spring Security: {@code POST /auth/logout}. */
@RestController
@RequestMapping("/auth")
class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextRepository;
    private final CurrentUser currentUser;
    private final SecurityContextHolderStrategy contextHolder = SecurityContextHolder.getContextHolderStrategy();

    AuthController(AuthService authService, AuthenticationManager authenticationManager,
            SecurityContextRepository contextRepository, CurrentUser currentUser) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.contextRepository = contextRepository;
        this.currentUser = currentUser;
    }

    /** Força a emissão do cookie XSRF-TOKEN; o front chama ao abrir. */
    @GetMapping("/csrf")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void csrf(@Parameter(hidden = true) CsrfToken token) {
        token.getToken();
    }

    @GetMapping("/status")
    StatusResponse status() {
        return new StatusResponse(authService.isSignupOpen());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    MeResponse register(@Valid @RequestBody RegisterRequest req,
            HttpServletRequest request, HttpServletResponse response) {
        User user = authService.register(req);
        signIn(user.getEmail(), req.password(), request, response);
        return MeResponse.of(user);
    }

    @PostMapping("/login")
    MeResponse login(@Valid @RequestBody LoginRequest req,
            HttpServletRequest request, HttpServletResponse response) {
        AuthUser principal = signIn(AuthService.normalizeEmail(req.email()), req.password(), request, response);
        return MeResponse.of(authService.get(principal.getId()));
    }

    @GetMapping("/me")
    MeResponse me() {
        return MeResponse.of(authService.get(currentUser.id()));
    }

    private AuthUser signIn(String email, String password, HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email, password));

        // Troca o id da sessão no login (proteção contra session fixation).
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        SecurityContext context = contextHolder.createEmptyContext();
        context.setAuthentication(auth);
        contextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
        return (AuthUser) auth.getPrincipal();
    }
}
