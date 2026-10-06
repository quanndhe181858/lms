package com.enterprise.lms.module.auth.dto;

/**
 * Response body for a successful login.
 * The refresh token is set as an HttpOnly cookie by the controller — NOT returned here.
 */
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        String email,
        String role
) {
    public static LoginResponse of(String accessToken, long expiresInMs, String email, String role) {
        return new LoginResponse(accessToken, "Bearer", expiresInMs / 1000, email, role);
    }
}
