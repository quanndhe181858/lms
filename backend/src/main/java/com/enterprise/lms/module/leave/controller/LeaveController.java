package com.enterprise.lms.module.leave.controller;

import com.enterprise.lms.module.leave.dto.LeaveSubmitRequest;
import com.enterprise.lms.module.leave.entity.LeaveBalance;
import com.enterprise.lms.module.leave.entity.LeaveRequest;
import com.enterprise.lms.module.leave.repository.LeaveBalanceRepository;
import com.enterprise.lms.module.leave.repository.LeaveRequestRepository;
import com.enterprise.lms.module.leave.service.LeaveRequestService;
import com.enterprise.lms.module.user.entity.User;
import com.enterprise.lms.module.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

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
}
