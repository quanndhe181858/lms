package com.enterprise.lms.security;

import com.enterprise.lms.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Responsible for signing, validating, and parsing JWT tokens.
 * Uses HMAC-SHA256 (HS256) with the configured secret.
 * NFR-4: JWT signatures validated using HMAC-SHA256 with secrets from environment config.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    // ── Key helpers ────────────────────────────────────────────────────────────

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)
        );
    }

    // ── Token generation ───────────────────────────────────────────────────────

    /**
     * Generate a short-lived access token embedding the subject (user email) and role.
     */
    public String generateAccessToken(String subject, String role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .claim("type", "access")
                .issuedAt(new Date(now))
                .expiration(new Date(now + jwtProperties.getAccessTokenExpiryMs()))
                .signWith(signingKey())
                .compact();
    }

    /**
     * Generate a long-lived refresh token (stored in HttpOnly cookie).
     */
    public String generateRefreshToken(String subject) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(subject)
                .claim("type", "refresh")
                .issuedAt(new Date(now))
                .expiration(new Date(now + jwtProperties.getRefreshTokenExpiryMs()))
                .signWith(signingKey())
                .compact();
    }

    // ── Token validation ───────────────────────────────────────────────────────

    /**
     * Returns true if the token is structurally valid, properly signed, and not expired.
     */
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    // ── Claims extraction ──────────────────────────────────────────────────────

    public String extractSubject(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public String extractTokenType(String token) {
        return parseClaims(token).get("type", String.class);
    }

    // ── Internal ───────────────────────────────────────────────────────────────

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
