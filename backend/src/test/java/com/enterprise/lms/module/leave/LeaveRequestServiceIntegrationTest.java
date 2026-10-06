package com.enterprise.lms.module.leave;

import com.enterprise.lms.module.leave.entity.LeaveBalance;
import com.enterprise.lms.module.leave.entity.LeaveLedgerEntry;
import com.enterprise.lms.module.leave.entity.LeaveRequest;
import com.enterprise.lms.module.leave.entity.LeaveType;
import com.enterprise.lms.module.leave.repository.LeaveBalanceRepository;
import com.enterprise.lms.module.leave.repository.LeaveLedgerEntryRepository;
import com.enterprise.lms.module.leave.repository.LeaveRequestRepository;
import com.enterprise.lms.module.leave.repository.LeaveTypeRepository;
import com.enterprise.lms.module.leave.service.LeaveRequestService;
import com.enterprise.lms.module.user.entity.User;
import com.enterprise.lms.module.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
class LeaveRequestServiceIntegrationTest {

    @Autowired
    private LeaveRequestService leaveRequestService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    private LeaveLedgerEntryRepository leaveLedgerEntryRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @BeforeEach
    void setUp() {
        leaveLedgerEntryRepository.deleteAll();
        leaveRequestRepository.deleteAll();
        leaveBalanceRepository.deleteAll();
        leaveTypeRepository.deleteAll();
    }

    @Test
    @DisplayName("Story 3.1: valid leave submission reserves balance and creates a submitted request")
    void submitLeave_validRequest_reservesBalanceAndPersistsRequest() {
        User user = userRepository.findByEmail("sarah.engineer@lms.local").orElseThrow();
        LeaveType leaveType = leaveTypeRepository.save(LeaveType.builder()
                .code("ANNUAL-REQUEST")
                .name("Annual Leave")
                .defaultDaysPerYear(new BigDecimal("20.00"))
                .isConfidentialAttachment(false)
                .requiresAttachmentDays(2)
                .isActive(true)
                .build());

        leaveBalanceRepository.save(LeaveBalance.builder()
                .userId(user.getId())
                .leaveTypeId(leaveType.getId())
                .accruedDays(new BigDecimal("5.00"))
                .carriedOverDays(new BigDecimal("0.00"))
                .usedDays(new BigDecimal("0.00"))
                .pendingDays(new BigDecimal("0.00"))
                .build());

        LocalDate startDate = LocalDate.now().plusDays(2);
        LocalDate endDate = startDate.plusDays(1);

        LeaveRequest request = leaveRequestService.submitLeave(
                user.getId(),
                leaveType.getId(),
                startDate,
                endDate,
                LeaveRequest.HalfDay.MORNING,
                LeaveRequest.HalfDay.AFTERNOON,
                "Family trip",
                "idem-valid-001",
                new BigDecimal("1.00")
        );

        assertThat(request.getStatus()).isEqualTo(LeaveRequest.Status.SUBMITTED);
        assertThat(request.getIdempotencyKey()).isEqualTo("idem-valid-001");

        LeaveBalance updatedBalance = leaveBalanceRepository.findByUserIdAndLeaveTypeId(user.getId(), leaveType.getId()).orElseThrow();
        assertThat(updatedBalance.getPendingDays()).isEqualByComparingTo("1.00");

        List<LeaveLedgerEntry> entries = leaveLedgerEntryRepository.findByUserIdAndLeaveTypeIdOrderByCreatedAtAsc(
                user.getId(), leaveType.getId());
        assertThat(entries).extracting(LeaveLedgerEntry::getEntryType)
                .contains(LeaveLedgerEntry.EntryType.RESERVATION);
    }

    @Test
    @DisplayName("Story 3.1: duplicate idempotency keys return the original request without double-booking")
    void submitLeave_duplicateIdempotencyKey_returnsOriginalRequestWithoutDoubleBooking() {
        User user = userRepository.findByEmail("sarah.engineer@lms.local").orElseThrow();
        LeaveType leaveType = leaveTypeRepository.save(LeaveType.builder()
                .code("SICK-REQUEST")
                .name("Sick Leave")
                .defaultDaysPerYear(new BigDecimal("10.00"))
                .isConfidentialAttachment(true)
                .requiresAttachmentDays(2)
                .isActive(true)
                .build());

        leaveBalanceRepository.save(LeaveBalance.builder()
                .userId(user.getId())
                .leaveTypeId(leaveType.getId())
                .accruedDays(new BigDecimal("10.00"))
                .carriedOverDays(new BigDecimal("0.00"))
                .usedDays(new BigDecimal("0.00"))
                .pendingDays(new BigDecimal("0.00"))
                .build());

        LeaveRequest first = leaveRequestService.submitLeave(
                user.getId(),
                leaveType.getId(),
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(5),
                LeaveRequest.HalfDay.MORNING,
                LeaveRequest.HalfDay.AFTERNOON,
                "Doctor appointment",
                "idem-duplicate-001",
                new BigDecimal("0.50")
        );

        LeaveRequest second = leaveRequestService.submitLeave(
                user.getId(),
                leaveType.getId(),
                LocalDate.now().plusDays(7),
                LocalDate.now().plusDays(7),
                LeaveRequest.HalfDay.MORNING,
                LeaveRequest.HalfDay.AFTERNOON,
                "Different reason",
                "idem-duplicate-001",
                new BigDecimal("0.50")
        );

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(leaveRequestRepository.findByIdempotencyKey("idem-duplicate-001").orElseThrow().getId())
                .isEqualTo(first.getId());
        assertThat(leaveRequestRepository.count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Story 3.4: approved leave cancelled before start date releases the used days back to balance")
    void cancelLeaveRequest_beforeStart_reversesUsedDaysAndCancelsRequest() {
        User user = userRepository.findByEmail("sarah.engineer@lms.local").orElseThrow();
        LeaveType leaveType = leaveTypeRepository.save(LeaveType.builder()
                .code("ANNUAL-CANCEL-PRE")
                .name("Annual Leave")
                .defaultDaysPerYear(new BigDecimal("20.00"))
                .isConfidentialAttachment(false)
                .requiresAttachmentDays(0)
                .isActive(true)
                .build());

        leaveBalanceRepository.save(LeaveBalance.builder()
                .userId(user.getId())
                .leaveTypeId(leaveType.getId())
                .accruedDays(new BigDecimal("10.00"))
                .carriedOverDays(new BigDecimal("0.00"))
                .usedDays(new BigDecimal("2.00"))
                .pendingDays(new BigDecimal("0.00"))
                .build());

        LeaveRequest request = leaveRequestRepository.save(LeaveRequest.builder()
                .requestUuid("req-cancel-pre-001")
                .userId(user.getId())
                .leaveTypeId(leaveType.getId())
                .startDate(LocalDate.now().plusDays(7))
                .endDate(LocalDate.now().plusDays(9))
                .totalBillableDays(new BigDecimal("2.00"))
                .reason("Planned change")
                .status(LeaveRequest.Status.APPROVED)
                .assignedApproverId(user.getManager() != null ? user.getManager().getId() : user.getId())
                .isBackdated(false)
                .submittedAt(java.time.LocalDateTime.now())
                .build());

        LeaveRequest cancelled = leaveRequestService.cancelLeaveRequest(request.getId(), user.getId(), "Schedule changed");

        assertThat(cancelled.getStatus()).isEqualTo(LeaveRequest.Status.CANCELLED);
        LeaveBalance balance = leaveBalanceRepository.findByUserIdAndLeaveTypeId(user.getId(), leaveType.getId()).orElseThrow();
        assertThat(balance.getUsedDays()).isZero();
        assertThat(leaveLedgerEntryRepository.findByUserIdAndLeaveTypeIdOrderByCreatedAtAsc(user.getId(), leaveType.getId()))
                .extracting(LeaveLedgerEntry::getEntryType)
                .contains(LeaveLedgerEntry.EntryType.REVERSAL);
    }

    @Test
    @DisplayName("Story 3.4: approved leave on or after start date is moved to cancel requested")
    void cancelLeaveRequest_afterStart_setsCancelRequestedStatus() {
        User user = userRepository.findByEmail("sarah.engineer@lms.local").orElseThrow();
        LeaveType leaveType = leaveTypeRepository.save(LeaveType.builder()
                .code("ANNUAL-CANCEL-POST")
                .name("Annual Leave")
                .defaultDaysPerYear(new BigDecimal("20.00"))
                .isConfidentialAttachment(false)
                .requiresAttachmentDays(0)
                .isActive(true)
                .build());

        leaveBalanceRepository.save(LeaveBalance.builder()
                .userId(user.getId())
                .leaveTypeId(leaveType.getId())
                .accruedDays(new BigDecimal("10.00"))
                .carriedOverDays(new BigDecimal("0.00"))
                .usedDays(new BigDecimal("2.00"))
                .pendingDays(new BigDecimal("0.00"))
                .build());

        LeaveRequest request = leaveRequestRepository.save(LeaveRequest.builder()
                .requestUuid("req-cancel-post-001")
                .userId(user.getId())
                .leaveTypeId(leaveType.getId())
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(1))
                .totalBillableDays(new BigDecimal("2.00"))
                .reason("Unexpected leave")
                .status(LeaveRequest.Status.APPROVED)
                .assignedApproverId(user.getManager() != null ? user.getManager().getId() : user.getId())
                .isBackdated(false)
                .submittedAt(java.time.LocalDateTime.now())
                .build());

        LeaveRequest updated = leaveRequestService.cancelLeaveRequest(request.getId(), user.getId(), "Need to cut short");

        assertThat(updated.getStatus()).isEqualTo(LeaveRequest.Status.CANCEL_REQUESTED);
        LeaveBalance balance = leaveBalanceRepository.findByUserIdAndLeaveTypeId(user.getId(), leaveType.getId()).orElseThrow();
        assertThat(balance.getUsedDays()).isEqualByComparingTo("2.00");
    }
}
