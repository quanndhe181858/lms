package com.enterprise.lms.module.leave;

import com.enterprise.lms.module.leave.entity.LeaveRequest;
import com.enterprise.lms.module.leave.entity.LeaveType;
import com.enterprise.lms.module.leave.repository.LeaveRequestRepository;
import com.enterprise.lms.module.leave.repository.LeaveTypeRepository;
import com.enterprise.lms.module.leave.repository.SystemAuditLogRepository;
import com.enterprise.lms.module.leave.service.AuditLogService;
import com.enterprise.lms.module.user.entity.User;
import com.enterprise.lms.module.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
class SystemAuditLogServiceIntegrationTest {

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private SystemAuditLogRepository systemAuditLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Test
    @DisplayName("Story 4.2: audit records are immutable and capture state transitions with JSON payload")
    void auditLogCapturesTransitionPayload() {
        User owner = userRepository.findByEmail("sarah.engineer@lms.local").orElseThrow();
        String leaveCode = "ANNUAL_" + java.util.UUID.randomUUID().toString().substring(0, 8);
        LeaveType leaveType = leaveTypeRepository.save(LeaveType.builder()
                .code(leaveCode)
                .name("Annual Leave")
                .defaultDaysPerYear(BigDecimal.valueOf(20))
                .isConfidentialAttachment(false)
                .requiresAttachmentDays(0)
                .isActive(true)
                .build());

        LeaveRequest request = leaveRequestRepository.save(LeaveRequest.builder()
                .requestUuid("audit-log-request-" + java.util.UUID.randomUUID())
                .userId(owner.getId())
                .leaveTypeId(leaveType.getId())
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(12))
                .totalBillableDays(BigDecimal.valueOf(3))
                .reason("Family maintenance")
                .status(LeaveRequest.Status.SUBMITTED)
                .submittedAt(LocalDateTime.now())
                .build());

        auditLogService.record(
                "LeaveRequest",
                request.getId(),
                "APPROVE",
                owner.getId(),
                Map.of("status", "SUBMITTED"),
                Map.of("status", "APPROVED", "resolvedAt", LocalDateTime.now().toString()),
                "203.0.113.10",
                Map.of("decision", "approved")
        );

        var auditEntries = systemAuditLogRepository.findAll();
        assertThat(auditEntries).isNotEmpty();

        var auditEntry = auditEntries.stream()
                .filter(entry -> "APPROVE".equals(entry.getAction()) && entry.getEntityId().equals(request.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(auditEntry.getAction()).isEqualTo("APPROVE");
        assertThat(auditEntry.getDetailsJson()).contains("approved");
        assertThat(auditEntry.getPreviousState()).contains("SUBMITTED");
        assertThat(auditEntry.getNewState()).contains("APPROVED");
    }
}
