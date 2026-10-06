package com.enterprise.lms.module.leave;

import com.enterprise.lms.module.leave.entity.LeaveBalance;
import com.enterprise.lms.module.leave.entity.LeaveLedgerEntry;
import com.enterprise.lms.module.leave.entity.LeaveRequest;
import com.enterprise.lms.module.leave.entity.LeaveType;
import com.enterprise.lms.module.leave.repository.LeaveBalanceRepository;
import com.enterprise.lms.module.leave.repository.LeaveLedgerEntryRepository;
import com.enterprise.lms.module.leave.repository.LeaveRequestRepository;
import com.enterprise.lms.module.leave.repository.LeaveTypeRepository;
import com.enterprise.lms.module.leave.service.ApprovalService;
import com.enterprise.lms.module.leave.service.LeaveRequestService;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class ApprovalServiceIntegrationTest {

    @Autowired
    private ApprovalService approvalService;

    @Autowired
    private LeaveRequestService leaveRequestService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private LeaveLedgerEntryRepository leaveLedgerEntryRepository;

    @Test
    @DisplayName("Story 3.2: approving a request moves it from pending to used and appends a deduction ledger entry")
    void approveRequest_movesReservationToDeduction() {
        User employee = userRepository.findByEmail("sarah.engineer@lms.local").orElseThrow();
        User manager = userRepository.findByEmail("david.manager@lms.local").orElseThrow();

        LeaveType leaveType = leaveTypeRepository.save(LeaveType.builder()
                .code("APPROVAL-ANNUAL")
                .name("Annual Leave")
                .defaultDaysPerYear(new BigDecimal("20.00"))
                .isConfidentialAttachment(false)
                .requiresAttachmentDays(2)
                .isActive(true)
                .build());

        leaveBalanceRepository.save(LeaveBalance.builder()
                .userId(employee.getId())
                .leaveTypeId(leaveType.getId())
                .accruedDays(new BigDecimal("10.00"))
                .carriedOverDays(new BigDecimal("0.00"))
                .usedDays(new BigDecimal("0.00"))
                .pendingDays(new BigDecimal("0.00"))
                .build());

        LeaveRequest request = leaveRequestService.submitLeave(
                employee.getId(),
                leaveType.getId(),
                LocalDate.now().plusDays(7),
                LocalDate.now().plusDays(7),
                LeaveRequest.HalfDay.MORNING,
                LeaveRequest.HalfDay.AFTERNOON,
                "Approved leave",
                "approval-approve-001",
                new BigDecimal("1.00")
        );

        request.setAssignedApproverId(manager.getId());
        request = leaveRequestRepository.save(request);

        LeaveRequest approved = approvalService.actOnRequest(manager.getId(), request.getId(), ApprovalService.Decision.APPROVE, "Looks good");

        assertThat(approved.getStatus()).isEqualTo(LeaveRequest.Status.APPROVED);
        LeaveBalance balance = leaveBalanceRepository.findByUserIdAndLeaveTypeId(employee.getId(), leaveType.getId()).orElseThrow();
        assertThat(balance.getPendingDays()).isZero();
        assertThat(balance.getUsedDays()).isEqualByComparingTo("1.00");
        assertThat(leaveLedgerEntryRepository.findByUserIdAndLeaveTypeIdOrderByCreatedAtAsc(employee.getId(), leaveType.getId()))
                .extracting(LeaveLedgerEntry::getEntryType)
                .contains(LeaveLedgerEntry.EntryType.RESERVATION, LeaveLedgerEntry.EntryType.DEDUCTION);
    }

    @Test
    @DisplayName("Story 3.2: rejecting a request requires a reason and releases the reservation")
    void rejectRequest_requiresReasonAndReleasesReservation() {
        User employee = userRepository.findByEmail("sarah.engineer@lms.local").orElseThrow();
        User manager = userRepository.findByEmail("david.manager@lms.local").orElseThrow();

        LeaveType leaveType = leaveTypeRepository.save(LeaveType.builder()
                .code("APPROVAL-REJECT")
                .name("Annual Leave")
                .defaultDaysPerYear(new BigDecimal("20.00"))
                .isConfidentialAttachment(false)
                .requiresAttachmentDays(2)
                .isActive(true)
                .build());

        leaveBalanceRepository.save(LeaveBalance.builder()
                .userId(employee.getId())
                .leaveTypeId(leaveType.getId())
                .accruedDays(new BigDecimal("10.00"))
                .carriedOverDays(new BigDecimal("0.00"))
                .usedDays(new BigDecimal("0.00"))
                .pendingDays(new BigDecimal("0.00"))
                .build());

        LeaveRequest request = leaveRequestService.submitLeave(
                employee.getId(),
                leaveType.getId(),
                LocalDate.now().plusDays(9),
                LocalDate.now().plusDays(9),
                LeaveRequest.HalfDay.MORNING,
                LeaveRequest.HalfDay.AFTERNOON,
                "Rejected leave",
                "approval-reject-001",
                new BigDecimal("1.00")
        );

        request.setAssignedApproverId(manager.getId());
        request = leaveRequestRepository.save(request);

        LeaveRequest rejected = approvalService.actOnRequest(manager.getId(), request.getId(), ApprovalService.Decision.REJECT, "Waiting for another date");

        assertThat(rejected.getStatus()).isEqualTo(LeaveRequest.Status.REJECTED);
        LeaveBalance balance = leaveBalanceRepository.findByUserIdAndLeaveTypeId(employee.getId(), leaveType.getId()).orElseThrow();
        assertThat(balance.getPendingDays()).isZero();
        assertThat(balance.getUsedDays()).isZero();
    }
}
