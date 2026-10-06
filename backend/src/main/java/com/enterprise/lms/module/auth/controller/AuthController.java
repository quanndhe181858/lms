package com.enterprise.lms.module.auth.controller;

import com.enterprise.lms.module.auth.dto.LoginRequest;
import com.enterprise.lms.module.auth.dto.LoginResponse;
import com.enterprise.lms.module.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.Map;

/**
 * Authentication endpoints — publicly accessible (no JWT required).
 *
 * Story 1.2:
 *  POST /api/v1/auth/login   → access token in body + refresh token in HttpOnly cookie
 *  POST /api/v1/auth/refresh → new access token from HttpOnly cookie
 *  POST /api/v1/auth/logout  → clears the refresh cookie
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refreshToken";

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        AuthService.AuthResult result = authService.login(request);
        setRefreshCookie(response, result.refreshToken(), result.refreshTokenExpiryMs());
        return ResponseEntity.ok(result.response());
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "MISSING_REFRESH_TOKEN");
        }
        LoginResponse loginResponse = authService.refresh(refreshToken);
        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletResponse response) {
        // Clear the HttpOnly refresh cookie
        Cookie cookie = new Cookie(REFRESH_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/v1/auth/refresh");
        cookie.setMaxAge(0); // Immediately expire
        response.addCookie(cookie);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void setRefreshCookie(HttpServletResponse response, String refreshToken, long expiryMs) {
        Cookie cookie = new Cookie(REFRESH_COOKIE_NAME, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);              // Enforce HTTPS (NFR-6)
        cookie.setPath("/api/v1/auth/refresh");
        cookie.setMaxAge((int) (expiryMs / 1000));
        response.addCookie(cookie);
    }
}
