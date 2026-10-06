package com.enterprise.lms.module.user.controller;

import com.enterprise.lms.module.user.dto.UserProfileResponse;
import com.enterprise.lms.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * User profile endpoints.
 * Story 1.3 — AC: GET /api/v1/users/me returns profile, role, department, manager.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Returns the authenticated user's own profile.
     * Requires any authenticated role (EMPLOYEE, MANAGER, HR_ADMIN).
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal UserDetails principal
    ) {
        return ResponseEntity.ok(userService.getMyProfile(principal.getUsername()));
    }
}
