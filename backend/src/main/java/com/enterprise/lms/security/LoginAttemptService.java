package com.enterprise.lms.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks consecutive failed login attempts per email (in-memory).
 * Story 1.2 — AC: 5 consecutive failures lock the account for 15 minutes (HTTP 423).
 *
 * NOTE: For multi-node deployments, replace ConcurrentHashMap with Redis-backed storage.
 */
@Slf4j
@Service
public class LoginAttemptService {

    private record AttemptRecord(int count, Instant lockedUntil) {}

    private final int maxAttempts;
    private final long lockoutDurationMinutes;
    private final Map<String, AttemptRecord> attemptStore = new ConcurrentHashMap<>();

    public LoginAttemptService(
            @Value("${app.security.max-failed-attempts:5}") int maxAttempts,
            @Value("${app.security.lockout-duration-minutes:15}") long lockoutDurationMinutes
    ) {
        this.maxAttempts = maxAttempts;
        this.lockoutDurationMinutes = lockoutDurationMinutes;
    }

    /** Call after a successful login to clear the failure counter. */
    public void loginSucceeded(String email) {
        attemptStore.remove(email);
    }

    /** Call after each failed login attempt. */
    public void loginFailed(String email) {
        AttemptRecord current = attemptStore.getOrDefault(email, new AttemptRecord(0, null));
        int newCount = current.count() + 1;
        Instant lockUntil = newCount >= maxAttempts
                ? Instant.now().plusSeconds(lockoutDurationMinutes * 60)
                : null;
        attemptStore.put(email, new AttemptRecord(newCount, lockUntil));
        log.warn("Failed login attempt #{} for email: {}", newCount, email);
    }

    /**
     * Returns true when the account is currently locked (enough failures AND still within lockout window).
     */
    public boolean isLocked(String email) {
        AttemptRecord record = attemptStore.get(email);
        if (record == null) return false;
        if (record.count() < maxAttempts) return false;
        if (record.lockedUntil() != null && Instant.now().isAfter(record.lockedUntil())) {
            // Lockout window expired — clear so user can try again
            attemptStore.remove(email);
            return false;
        }
        return true;
    }

    /** Remaining lockout seconds for the given email, or 0 if not locked. */
    public long remainingLockoutSeconds(String email) {
        AttemptRecord record = attemptStore.get(email);
        if (record == null || record.lockedUntil() == null) return 0;
        long remaining = record.lockedUntil().getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0, remaining);
    }
}
