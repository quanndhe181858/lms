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
    private final com.enterprise.lms.module.leave.repository.LeaveTypeRepository leaveTypeRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'HR_ADMIN')")
    public ResponseEntity<List<java.util.Map<String, Object>>> getPendingApprovals(
            @AuthenticationPrincipal UserDetails principal
    ) {
        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND"));

        List<LeaveRequest> requests;
        if (user.getRole() == com.enterprise.lms.module.user.entity.Role.ROLE_HR_ADMIN) {
            requests = approvalService.getAllPendingApprovals();
        } else {
            requests = approvalService.getPendingApprovals(user.getId());
        }

        List<Long> userIds = requests.stream().map(LeaveRequest::getUserId).distinct().collect(java.util.stream.Collectors.toList());
        java.util.Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, java.util.function.Function.identity()));

        java.util.Map<Long, com.enterprise.lms.module.leave.entity.LeaveType> leaveTypeMap = leaveTypeRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(com.enterprise.lms.module.leave.entity.LeaveType::getId, java.util.function.Function.identity()));

        List<java.util.Map<String, Object>> response = requests.stream().map(r -> {
            java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("id", r.getId());
            map.put("requestUuid", r.getRequestUuid());
            map.put("userId", r.getUserId());
            map.put("leaveTypeId", r.getLeaveTypeId());
            map.put("startDate", r.getStartDate());
            map.put("endDate", r.getEndDate());
            map.put("startHalf", r.getStartHalf());
            map.put("endHalf", r.getEndHalf());
            map.put("totalBillableDays", r.getTotalBillableDays());
            map.put("reason", r.getReason());
            map.put("status", r.getStatus());
            map.put("assignedApproverId", r.getAssignedApproverId());
            map.put("isBackdated", r.isBackdated());
            map.put("backdated", r.isBackdated());
            map.put("submittedAt", r.getSubmittedAt());

            User requester = userMap.get(r.getUserId());
            if (requester != null) {
                map.put("requesterName", requester.getFullName());
                map.put("requesterEmail", requester.getEmail());
                map.put("departmentName", requester.getDepartment() != null ? requester.getDepartment().getName() : "");
            }

            com.enterprise.lms.module.leave.entity.LeaveType lt = leaveTypeMap.get(r.getLeaveTypeId());
            if (lt != null) {
                map.put("leaveTypeName", lt.getName());
            }

            return map;
        }).collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(response);
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
