package com.enterprise.lms.module.user.dto;

/**
 * Profile response for GET /api/v1/users/me
 * Story 1.3 — AC: returns authenticated user's profile, role, department, and direct manager details.
 */
public record UserProfileResponse(
        Long id,
        String email,
        String fullName,
        String role,
        String employmentStatus,
        DepartmentSummary department,
        ManagerSummary manager
) {
    public record DepartmentSummary(Long id, String name, String code) {}
    public record ManagerSummary(Long id, String email, String fullName) {}
}
