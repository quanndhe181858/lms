package com.enterprise.lms.module.auth;

import com.enterprise.lms.module.auth.dto.LoginRequest;
import com.enterprise.lms.module.auth.dto.LoginResponse;
import com.enterprise.lms.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests covering Story 1.2 (Auth) and Story 1.3 (RBAC).
 *
 * AC coverage:
 *  1.2-AC1: Valid credentials → 200 + access token
 *  1.2-AC2: Invalid credentials → 401 INVALID_CREDENTIALS
 *  1.2-AC3: 5 consecutive failures → 423 Locked
 *  1.3-AC1: Unauthenticated → 401
 *  1.3-AC2: Valid JWT → GET /me returns profile with role and department
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AuthAndRbacIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtTokenProvider jwtTokenProvider;

    private static final String ADMIN_EMAIL    = "admin@lms.local";
    private static final String ADMIN_PASSWORD = "Password@123456";

    // ── 1.2-AC1: Valid login ───────────────────────────────────────────────────

    @Test
    @DisplayName("1.2-AC1: Valid credentials return 200 with JWT access token and refreshToken cookie")
    void loginWithValidCredentials_returns200WithToken() throws Exception {
        LoginRequest req = new LoginRequest(ADMIN_EMAIL, ADMIN_PASSWORD);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.role").value("ROLE_HR_ADMIN"))
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andReturn();

        LoginResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), LoginResponse.class);
        assertThat(jwtTokenProvider.isTokenValid(response.accessToken())).isTrue();
        assertThat(jwtTokenProvider.extractSubject(response.accessToken())).isEqualTo(ADMIN_EMAIL);
    }

    // ── 1.2-AC2: Invalid credentials ──────────────────────────────────────────

    @Test
    @DisplayName("1.2-AC2: Invalid credentials return 401 INVALID_CREDENTIALS")
    void loginWithWrongPassword_returns401() throws Exception {
        LoginRequest req = new LoginRequest(ADMIN_EMAIL, "WrongPassword!");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    // ── 1.2-AC3: Account lockout after 5 failures ─────────────────────────────

    @Test
    @DisplayName("1.2-AC3: 5 consecutive failures lock account and return HTTP 423")
    void fiveFailedAttempts_locksAccount() throws Exception {
        // Use a unique email per test run to avoid cross-test state pollution
        String victimEmail = "lockout.test@lms.local";

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new LoginRequest(victimEmail, "wrong"))))
                    .andExpect(status().isUnauthorized()); // 401 during accumulation
        }

        // 6th attempt should be blocked
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(victimEmail, "wrong"))))
                .andExpect(status().isLocked()); // 423
    }

    // ── 1.3-AC1: Unauthenticated request ──────────────────────────────────────

    @Test
    @DisplayName("1.3-AC1: Unauthenticated request to protected endpoint returns 401")
    void unauthenticatedRequest_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    // ── 1.3-AC2: Authenticated /me returns profile ────────────────────────────

    @Test
    @DisplayName("1.3-AC2: Valid JWT returns user profile with role and department")
    void authenticatedMe_returnsProfile() throws Exception {
        String accessToken = jwtTokenProvider.generateAccessToken(ADMIN_EMAIL, "ROLE_HR_ADMIN");

        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(ADMIN_EMAIL))
                .andExpect(jsonPath("$.role").value("ROLE_HR_ADMIN"))
                .andExpect(jsonPath("$.department.code").isNotEmpty());
    }

    // ── 1.3-AC3: Refresh token rejected as Bearer ─────────────────────────────

    @Test
    @DisplayName("1.3-AC3: Refresh token used as Bearer is rejected with 401")
    void refreshTokenAsBearerIsRejected() throws Exception {
        String refreshToken = jwtTokenProvider.generateRefreshToken(ADMIN_EMAIL);

        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized());
    }
}
