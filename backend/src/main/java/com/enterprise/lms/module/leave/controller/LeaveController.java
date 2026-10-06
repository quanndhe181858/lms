package com.enterprise.lms.module.leave.controller;

import com.enterprise.lms.module.leave.dto.LeaveSubmitRequest;
import com.enterprise.lms.module.leave.entity.LeaveBalance;
import com.enterprise.lms.module.leave.entity.LeaveRequest;
import com.enterprise.lms.module.leave.repository.LeaveBalanceRepository;
import com.enterprise.lms.module.leave.repository.LeaveRequestRepository;
import com.enterprise.lms.module.leave.service.LeaveRequestService;
import com.enterprise.lms.module.user.entity.Role;
import com.enterprise.lms.module.user.entity.User;
import com.enterprise.lms.module.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveRequestService leaveRequestService;
    private final UserRepository userRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;

    /**
     * Submit a new leave request.
     * POST /api/v1/leaves/submit
     */
    @PostMapping("/submit")
    public ResponseEntity<LeaveRequest> submitLeave(
            @AuthenticationPrincipal UserDetails principal,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody LeaveSubmitRequest request
    ) {
        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND"));

        LeaveRequest submitted = leaveRequestService.submitLeave(
                user.getId(),
                request.getLeaveTypeId(),
                request.getStartDate(),
                request.getEndDate(),
                request.getStartHalf(),
                request.getEndHalf(),
                request.getReason(),
                idempotencyKey,
                request.getTotalBillableDays()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(submitted);
    }

    /**
     * Get the authenticated user's leave requests.
     * GET /api/v1/leaves/my
     */
    @GetMapping("/my")
    public ResponseEntity<List<LeaveRequest>> getMyLeaveRequests(
            @AuthenticationPrincipal UserDetails principal
    ) {
        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND"));
        List<LeaveRequest> requests = leaveRequestRepository.findByUserIdOrderBySubmittedAtDesc(user.getId());
        return ResponseEntity.ok(requests);
    }

    /**
     * Get the authenticated user's leave balances.
     * GET /api/v1/leaves/balances
     */
    @GetMapping("/balances")
    public ResponseEntity<List<LeaveBalance>> getMyBalances(
            @AuthenticationPrincipal UserDetails principal
    ) {
        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND"));
        List<LeaveBalance> balances = leaveBalanceRepository.findByUserIdOrderByLeaveTypeId(user.getId());
        return ResponseEntity.ok(balances);
    }

    /**
     * Get team schedule: leave requests for users visible to the authenticated user,
     * overlapping the given date window.
     * GET /api/v1/leaves/team-schedule?start=YYYY-MM-DD&end=YYYY-MM-DD
     */
    @GetMapping("/team-schedule")
    public ResponseEntity<List<LeaveRequest>> getTeamSchedule(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        User me = userRepository.findWithManagerByEmail(principal.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND"));

        List<Long> userIds;
        if (me.getRole() == Role.ROLE_HR_ADMIN) {
            // HR sees everyone
            userIds = userRepository.findByActiveTrue()
                    .stream().map(User::getId).collect(Collectors.toList());
        } else if (me.getRole() == Role.ROLE_MANAGER) {
            // Manager sees their direct reports + themselves
            userIds = userRepository.findByManagerId(me.getId())
                    .stream().map(User::getId).collect(Collectors.toList());
            userIds.add(me.getId());
        } else {
            // Employee sees department colleagues
            Long deptId = me.getDepartment().getId();
            userIds = userRepository.findByActiveTrue().stream()
                    .filter(u -> u.getDepartment() != null && u.getDepartment().getId().equals(deptId))
                    .map(User::getId)
                    .collect(Collectors.toList());
        }

        Collection<LeaveRequest.Status> visibleStatuses = List.of(
                LeaveRequest.Status.SUBMITTED,
                LeaveRequest.Status.ESCALATED,
                LeaveRequest.Status.APPROVED
        );
        List<LeaveRequest> scheduleEntries = leaveRequestRepository.findTeamSchedule(userIds, visibleStatuses, start, end);
        return ResponseEntity.ok(scheduleEntries);
    }

    /**
     * Get team members: users visible to the authenticated user.
     * GET /api/v1/leaves/team-members
     */
    @GetMapping("/team-members")
    public ResponseEntity<List<java.util.Map<String, Object>>> getTeamMembers(
            @AuthenticationPrincipal UserDetails principal
    ) {
        User me = userRepository.findWithManagerByEmail(principal.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND"));

        List<User> teamUsers;
        if (me.getRole() == Role.ROLE_HR_ADMIN) {
            teamUsers = userRepository.findByActiveTrue();
        } else if (me.getRole() == Role.ROLE_MANAGER) {
            teamUsers = new java.util.ArrayList<>(userRepository.findByManagerId(me.getId()));
            teamUsers.add(me);
        } else {
            Long deptId = me.getDepartment().getId();
            teamUsers = userRepository.findByActiveTrue().stream()
                    .filter(u -> u.getDepartment() != null && u.getDepartment().getId().equals(deptId))
                    .collect(Collectors.toList());
        }

        List<java.util.Map<String, Object>> result = teamUsers.stream().map(u -> {
            java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("id", u.getId());
            map.put("fullName", u.getFullName());
            map.put("role", u.getRole() != null ? u.getRole().name() : "ROLE_EMPLOYEE");
            map.put("employmentStatus", u.getEmploymentStatus() != null ? u.getEmploymentStatus().name() : "PERMANENT");
            map.put("department", u.getDepartment() != null ? u.getDepartment().getName() : "");
            map.put("active", u.isActive());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}

