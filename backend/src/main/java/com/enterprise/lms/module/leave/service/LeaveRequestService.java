package com.enterprise.lms.module.leave.service;

import com.enterprise.lms.module.leave.entity.LeaveRequest;
import com.enterprise.lms.module.leave.repository.LeaveRequestRepository;
import com.enterprise.lms.module.user.entity.User;
import com.enterprise.lms.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final UserRepository userRepository;
    private final LeaveBalanceService leaveBalanceService;
    private final AuditLogService auditLogService;

    @Transactional
    public LeaveRequest submitLeave(
            Long userId,
            Long leaveTypeId,
            LocalDate startDate,
            LocalDate endDate,
            LeaveRequest.HalfDay startHalf,
            LeaveRequest.HalfDay endHalf,
            String reason,
            String idempotencyKey,
            BigDecimal totalBillableDays
    ) {
        validateInputs(userId, leaveTypeId, startDate, endDate, totalBillableDays, reason);

        if (startDate.isBefore(LocalDate.now())) {
            validateBackdatedRequest(reason);
        }

        String normalizedKey = (idempotencyKey == null || idempotencyKey.isBlank())
                ? UUID.randomUUID().toString()
                : idempotencyKey;

        LeaveRequest existing = leaveRequestRepository.findByIdempotencyKey(normalizedKey).orElse(null);
        if (existing != null) {
            return existing;
        }

        if (hasOverlap(userId, startDate, endDate)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "REQUEST_OVERLAP");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));

        LeaveRequest request = leaveRequestRepository.save(LeaveRequest.builder()
                .requestUuid(UUID.randomUUID().toString())
                .idempotencyKey(normalizedKey)
                .userId(userId)
                .leaveTypeId(leaveTypeId)
                .startDate(startDate)
                .endDate(endDate)
                .startHalf(startHalf == null ? LeaveRequest.HalfDay.MORNING : startHalf)
                .endHalf(endHalf == null ? LeaveRequest.HalfDay.AFTERNOON : endHalf)
                .totalBillableDays(totalBillableDays)
                .reason(reason)
                .status(LeaveRequest.Status.SUBMITTED)
                .assignedApproverId(user.getManager() != null ? user.getManager().getId() : userId)
                .isBackdated(startDate.isBefore(LocalDate.now()))
                .submittedAt(LocalDateTime.now())
                .build());

        leaveBalanceService.reserveBalance(userId, leaveTypeId, totalBillableDays, userId, request.getId());
        auditLogService.recordAsync(
                "LeaveRequest",
                request.getId(),
                "SUBMIT",
                userId,
                null,
                java.util.Map.of("status", request.getStatus().name(), "userId", userId),
                "system",
                java.util.Map.of("leaveTypeId", leaveTypeId, "billableDays", totalBillableDays)
        );
        return request;
    }

    @Transactional
    public LeaveRequest cancelLeaveRequest(Long requestId, Long actorUserId, String reason) {
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "REQUEST_NOT_FOUND"));

        if (request.getStatus() != LeaveRequest.Status.APPROVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "REQUEST_NOT_APPROVED");
        }

        if (request.getStartDate().isAfter(LocalDate.now())) {
            leaveBalanceService.reverseUsedDays(
                    request.getUserId(),
                    request.getLeaveTypeId(),
                    request.getTotalBillableDays(),
                    actorUserId,
                    request.getId()
            );
            LeaveRequest.Status previousStatus = request.getStatus();
            request.setStatus(LeaveRequest.Status.CANCELLED);
            request.setResolvedAt(LocalDateTime.now());
            request.setRejectionReason(null);
            LeaveRequest saved = leaveRequestRepository.save(request);
            auditLogService.recordAsync(
                    "LeaveRequest",
                    saved.getId(),
                    "CANCEL",
                    actorUserId,
                    java.util.Map.of("status", previousStatus.name()),
                    java.util.Map.of("status", saved.getStatus().name(), "resolvedAt", saved.getResolvedAt().toString()),
                    "system",
                    java.util.Map.of("reason", reason, "cancelledBeforeStartDate", true)
            );
            return saved;
        }

        if (reason != null && !reason.isBlank()) {
            request.setRejectionReason(reason);
        }
        LeaveRequest.Status previousStatus = request.getStatus();
        request.setStatus(LeaveRequest.Status.CANCEL_REQUESTED);
        request.setResolvedAt(LocalDateTime.now());
        request.setBackdated(request.getStartDate().isBefore(LocalDate.now()));
        LeaveRequest saved = leaveRequestRepository.save(request);
        auditLogService.recordAsync(
                "LeaveRequest",
                saved.getId(),
                "CANCEL",
                actorUserId,
                java.util.Map.of("status", previousStatus.name()),
                java.util.Map.of("status", saved.getStatus().name(), "resolvedAt", saved.getResolvedAt().toString()),
                "system",
                java.util.Map.of("reason", reason, "cancelRequested", true)
        );
        return saved;
    }

    private void validateBackdatedRequest(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "BACKDATED_REASON_REQUIRED");
        }
    }

    private void validateInputs(Long userId, Long leaveTypeId, LocalDate startDate, LocalDate endDate,
                                BigDecimal totalBillableDays, String reason) {
        if (userId == null || leaveTypeId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_USER_OR_LEAVE_TYPE");
        }
        if (startDate == null || endDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE");
        }
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE");
        }
        if (totalBillableDays == null || totalBillableDays.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_TOTAL_BILLABLE_DAYS");
        }
        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "REASON_REQUIRED");
        }
    }

    private boolean hasOverlap(Long userId, LocalDate startDate, LocalDate endDate) {
        return leaveRequestRepository.existsByUserIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                userId,
                List.of(LeaveRequest.Status.SUBMITTED, LeaveRequest.Status.ESCALATED, LeaveRequest.Status.APPROVED),
                endDate,
                startDate
        );
    }
}
