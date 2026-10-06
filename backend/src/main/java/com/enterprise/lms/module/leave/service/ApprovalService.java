package com.enterprise.lms.module.leave.service;

import com.enterprise.lms.module.leave.entity.LeaveRequest;
import com.enterprise.lms.module.leave.repository.LeaveRequestRepository;
import com.enterprise.lms.module.user.entity.Role;
import com.enterprise.lms.module.user.entity.User;
import com.enterprise.lms.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceService leaveBalanceService;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    public List<LeaveRequest> getPendingApprovals(Long managerId) {
        return leaveRequestRepository.findByAssignedApproverIdAndStatusIn(managerId, List.of(LeaveRequest.Status.SUBMITTED, LeaveRequest.Status.ESCALATED));
    }

    public List<LeaveRequest> getAllPendingApprovals() {
        return leaveRequestRepository.findByStatusIn(List.of(LeaveRequest.Status.SUBMITTED, LeaveRequest.Status.ESCALATED));
    }

    @Transactional
    public LeaveRequest actOnRequest(Long managerId, Long requestId, Decision decision, String decisionReason) {
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "REQUEST_NOT_FOUND"));

        User approver = userRepository.findById(managerId).orElse(null);
        boolean isHrAdmin = approver != null && approver.getRole() == Role.ROLE_HR_ADMIN;

        if (!managerId.equals(request.getAssignedApproverId()) && !isHrAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "NOT_ASSIGNED_APPROVER");
        }

        if (!List.of(LeaveRequest.Status.SUBMITTED, LeaveRequest.Status.ESCALATED).contains(request.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "REQUEST_NOT_PENDING_APPROVAL");
        }

        if (decision == Decision.REJECT && (decisionReason == null || decisionReason.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "REJECTION_REASON_REQUIRED");
        }

        LeaveRequest.Status previousStatus = request.getStatus();

        if (decision == Decision.APPROVE) {
            leaveBalanceService.finalizeApprovedReservation(
                    request.getUserId(),
                    request.getLeaveTypeId(),
                    request.getTotalBillableDays(),
                    managerId,
                    request.getId()
            );
            request.setStatus(LeaveRequest.Status.APPROVED);
            request.setResolvedAt(LocalDateTime.now());
            request.setRejectionReason(null);
        } else {
            leaveBalanceService.releaseReservation(
                    request.getUserId(),
                    request.getLeaveTypeId(),
                    request.getTotalBillableDays(),
                    managerId,
                    request.getId()
            );
            request.setStatus(LeaveRequest.Status.REJECTED);
            request.setResolvedAt(LocalDateTime.now());
            request.setRejectionReason(decisionReason);
        }

        LeaveRequest saved = leaveRequestRepository.save(request);
        auditLogService.recordAsync(
                "LeaveRequest",
                saved.getId(),
                decision == Decision.APPROVE ? "APPROVE" : "REJECT",
                managerId,
                java.util.Map.of("status", previousStatus.name()),
                java.util.Map.of("status", saved.getStatus().name(), "resolvedAt", saved.getResolvedAt().toString()),
                "system",
                java.util.Map.of("decision", decision.name(), "reason", decisionReason)
        );
        return saved;
    }

    public enum Decision {
        APPROVE,
        REJECT
    }
}
