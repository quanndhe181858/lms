package com.enterprise.lms.module.user.service;

import com.enterprise.lms.module.user.dto.UserProfileResponse;
import com.enterprise.lms.module.user.entity.User;
import com.enterprise.lms.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * User profile business logic.
 * Story 1.3 — exposes authenticated user's profile context.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(String email) {
        // JOIN FETCH manager so we can safely map the reporting line
        User user = userRepository.findWithManagerByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));

        return toProfileResponse(user);
    }

    // ── Mapping ────────────────────────────────────────────────────────────────

    private UserProfileResponse toProfileResponse(User user) {
        UserProfileResponse.DepartmentSummary dept = null;
        if (user.getDepartment() != null) {
            dept = new UserProfileResponse.DepartmentSummary(
                    user.getDepartment().getId(),
                    user.getDepartment().getName(),
                    user.getDepartment().getDepartmentCode()
            );
        }

        UserProfileResponse.ManagerSummary manager = null;
        if (user.getManager() != null) {
            manager = new UserProfileResponse.ManagerSummary(
                    user.getManager().getId(),
                    user.getManager().getEmail(),
                    user.getManager().getFullName()
            );
        }

        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                user.getEmploymentStatus().name(),
                dept,
                manager
        );
    }
}
