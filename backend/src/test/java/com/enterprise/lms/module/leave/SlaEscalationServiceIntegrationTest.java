package com.enterprise.lms.module.leave;

import com.enterprise.lms.module.leave.entity.LeaveRequest;
import com.enterprise.lms.module.leave.entity.LeaveType;
import com.enterprise.lms.module.leave.repository.LeaveRequestRepository;
import com.enterprise.lms.module.leave.repository.LeaveTypeRepository;
import com.enterprise.lms.module.leave.service.SlaEscalationService;
import com.enterprise.lms.module.user.entity.User;
import com.enterprise.lms.module.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class SlaEscalationServiceIntegrationTest {

    @Autowired
    private SlaEscalationService slaEscalationService;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Test
    @DisplayName("Story 3.3: SLA scheduler warns after 24 business hours and escalates after 48 business hours")
    void processSla_warnsAndEscalatesPendingRequests() {
        User employee = userRepository.findByEmail("sarah.engineer@lms.local").orElseThrow();
        User manager = userRepository.findByEmail("david.manager@lms.local").orElseThrow();

        LeaveType leaveType = leaveTypeRepository.save(LeaveType.builder()
                .code("SLA-TEST")
                .name("Annual Leave")
                .defaultDaysPerYear(new BigDecimal("20.00"))
                .isConfidentialAttachment(false)
                .requiresAttachmentDays(2)
                .isActive(true)
                .build());

        LeaveRequest request = leaveRequestRepository.save(LeaveRequest.builder()
                .requestUuid("sla-request-001")
                .userId(employee.getId())
                .leaveTypeId(leaveType.getId())
                .startDate(LocalDate.now().plusDays(7))
                .endDate(LocalDate.now().plusDays(7))
                .startHalf(LeaveRequest.HalfDay.MORNING)
                .endHalf(LeaveRequest.HalfDay.AFTERNOON)
                .totalBillableDays(new BigDecimal("1.00"))
                .reason("Holiday")
                .status(LeaveRequest.Status.SUBMITTED)
                .assignedApproverId(manager.getId())
                .submittedAt(LocalDateTime.now().minusDays(8).withHour(8).withMinute(0).withSecond(0).withNano(0))
                .build());

        slaEscalationService.processPendingSla();

        LeaveRequest afterReminder = leaveRequestRepository.findById(request.getId()).orElseThrow();
        assertThat(afterReminder.isReminderSent()).isTrue();

        LeaveRequest escalated = leaveRequestRepository.save(LeaveRequest.builder()
                .requestUuid("sla-request-002")
                .userId(employee.getId())
                .leaveTypeId(leaveType.getId())
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(10))
                .startHalf(LeaveRequest.HalfDay.MORNING)
                .endHalf(LeaveRequest.HalfDay.AFTERNOON)
                .totalBillableDays(new BigDecimal("1.00"))
                .reason("Holiday")
                .status(LeaveRequest.Status.SUBMITTED)
                .assignedApproverId(manager.getId())
                .submittedAt(LocalDateTime.now().minusDays(15).withHour(8).withMinute(0).withSecond(0).withNano(0))
                .build());

        slaEscalationService.processPendingSla();

        LeaveRequest afterEscalation = leaveRequestRepository.findById(escalated.getId()).orElseThrow();
        assertThat(afterEscalation.getStatus()).isEqualTo(LeaveRequest.Status.ESCALATED);
        assertThat(afterEscalation.getEscalatedAt()).isNotNull();
    }
}
