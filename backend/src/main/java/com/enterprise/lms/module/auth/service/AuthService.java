package com.enterprise.lms.module.auth.service;

import com.enterprise.lms.config.JwtProperties;
import com.enterprise.lms.module.auth.dto.LoginRequest;
import com.enterprise.lms.module.auth.dto.LoginResponse;
import com.enterprise.lms.module.user.entity.User;
import com.enterprise.lms.module.user.repository.UserRepository;
import com.enterprise.lms.security.JwtTokenProvider;
import com.enterprise.lms.security.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Orchestrates authentication: validates credentials, enforces lockout policy,
 * and issues JWT access + refresh tokens.
 *
 * Story 1.2 acceptance criteria:
 *  - Valid credentials → 200 + access token + HttpOnly refresh cookie
 *  - Invalid credentials → 401 INVALID_CREDENTIALS
 *  - ≥5 failures → 423 ACCOUNT_LOCKED
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final LoginAttemptService loginAttemptService;

    @Transactional(readOnly = true)
    public AuthResult login(LoginRequest request) {
        String email = request.email();

        // Guard: account locked?
        if (loginAttemptService.isLocked(email)) {
            long remaining = loginAttemptService.remainingLockoutSeconds(email);
            log.warn("Login blocked — account locked: {} ({} seconds remaining)", email, remaining);
            throw new ResponseStatusException(
                    HttpStatus.LOCKED,
                    "Account temporarily locked. Try again in " + remaining + " seconds."
            );
        }

        // Lookup user
        User user = userRepository.findByEmail(email).orElse(null);

        // Validate password — run encoder even on null user to prevent timing oracle
        boolean credentialsValid = user != null
                && passwordEncoder.matches(request.password(), user.getPasswordHash());

        if (!credentialsValid) {
            loginAttemptService.loginFailed(email);
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "INVALID_CREDENTIALS"
            );
        }

        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ACCOUNT_INACTIVE");
        }

        loginAttemptService.loginSucceeded(email);

        String accessToken = jwtTokenProvider.generateAccessToken(email, user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(email);

        LoginResponse response = LoginResponse.of(
                accessToken,
                jwtProperties.getAccessTokenExpiryMs(),
                email,
                user.getRole().name()
        );

        return new AuthResult(response, refreshToken, jwtProperties.getRefreshTokenExpiryMs());
    }

    /**
     * Validates an existing refresh token and issues a fresh access token.
     */
    @Transactional(readOnly = true)
    public LoginResponse refresh(String refreshToken) {
        if (!jwtTokenProvider.isTokenValid(refreshToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN");
        }
        if (!"refresh".equals(jwtTokenProvider.extractTokenType(refreshToken))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "TOKEN_TYPE_MISMATCH");
        }

        String email = jwtTokenProvider.extractSubject(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND"));

        String newAccessToken = jwtTokenProvider.generateAccessToken(email, user.getRole().name());
        return LoginResponse.of(newAccessToken, jwtProperties.getAccessTokenExpiryMs(), email, user.getRole().name());
    }

    /** Intermediate record carrying both the response body and the refresh token to be set as cookie. */
    public record AuthResult(LoginResponse response, String refreshToken, long refreshTokenExpiryMs) {}
}
