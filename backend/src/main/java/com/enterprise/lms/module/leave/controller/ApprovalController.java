package com.enterprise.lms.module.leave.controller;

import com.enterprise.lms.module.leave.dto.ApprovalDecisionRequest;
import com.enterprise.lms.module.leave.entity.LeaveRequest;
import com.enterprise.lms.module.leave.service.ApprovalService;
import com.enterprise.lms.module.user.entity.User;
import com.enterprise.lms.module.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Approval endpoints for managers and HR admins.
 * GET  /api/v1/approvals          — list pending approvals for the authenticated approver
 * POST /api/v1/approvals/{id}/act — approve or reject a specific leave request
 */
@RestController
@RequestMapping("/api/v1/approvals")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'HR_ADMIN')")
    public ResponseEntity<List<LeaveRequest>> getPendingApprovals(
            @AuthenticationPrincipal UserDetails principal
    ) {
        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND"));
        return ResponseEntity.ok(approvalService.getPendingApprovals(user.getId()));
    }

    @PostMapping("/{id}/act")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR_ADMIN')")
    public ResponseEntity<LeaveRequest> actOnRequest(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody ApprovalDecisionRequest request
    ) {
        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND"));

        LeaveRequest result = approvalService.actOnRequest(
                user.getId(),
                id,
                request.getDecision(),
                request.getDecisionReason()
        );
        return ResponseEntity.ok(result);
    }
}
