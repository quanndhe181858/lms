package com.enterprise.lms.module.leave;

import com.enterprise.lms.module.leave.entity.LeaveBalance;
import com.enterprise.lms.module.leave.entity.LeaveLedgerEntry;
import com.enterprise.lms.module.leave.entity.LeaveType;
import com.enterprise.lms.module.leave.repository.LeaveBalanceRepository;
import com.enterprise.lms.module.leave.repository.LeaveLedgerEntryRepository;
import com.enterprise.lms.module.leave.repository.LeaveTypeRepository;
import com.enterprise.lms.module.leave.service.LeaveBalanceService;
import com.enterprise.lms.module.user.entity.User;
import com.enterprise.lms.module.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
class LeaveBalanceServiceIntegrationTest {

    @Autowired
    private LeaveBalanceService leaveBalanceService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    private LeaveLedgerEntryRepository leaveLedgerEntryRepository;

    @Test
    @DisplayName("Story 2.1: reservation creates ledger entry and preserves the accurate remaining balance")
    void reserveBalance_createsLedgerEntryAndMaintainsBalanceIntegrity() {
        User user = userRepository.findByEmail("sarah.engineer@lms.local").orElseThrow();

        LeaveType leaveType = leaveTypeRepository.save(LeaveType.builder()
                .code("ANNUAL")
                .name("Annual Leave")
                .defaultDaysPerYear(new BigDecimal("20.00"))
                .isConfidentialAttachment(false)
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

        BigDecimal remaining = leaveBalanceService.reserveBalance(
                user.getId(),
                leaveType.getId(),
                new BigDecimal("1.50"),
                user.getId(),
                5001L
        );

        assertThat(remaining).isEqualByComparingTo("8.50");

        List<LeaveLedgerEntry> ledgerEntries = leaveLedgerEntryRepository.findByUserIdAndLeaveTypeIdOrderByCreatedAtAsc(
                user.getId(), leaveType.getId());
        assertThat(ledgerEntries).hasSize(1);
        assertThat(ledgerEntries.get(0).getEntryType()).isEqualTo(LeaveLedgerEntry.EntryType.RESERVATION);
        assertThat(ledgerEntries.get(0).getAmount()).isEqualByComparingTo("-1.50");
        assertThat(ledgerEntries.get(0).getBalanceAfter()).isEqualByComparingTo("8.50");
    }

    @Test
    @DisplayName("Story 2.1: concurrent reservations on one balance allow only a single successful reservation")
    void reserveBalance_concurrentRequests_onlyOneSucceeds() throws Exception {
        User user = userRepository.findByEmail("sarah.engineer@lms.local").orElseThrow();

        LeaveType leaveType = leaveTypeRepository.save(LeaveType.builder()
                .code("SICK")
                .name("Sick Leave")
                .defaultDaysPerYear(new BigDecimal("10.00"))
                .isConfidentialAttachment(true)
                .requiresAttachmentDays(1)
                .isActive(true)
                .build());

        leaveBalanceRepository.save(LeaveBalance.builder()
                .userId(user.getId())
                .leaveTypeId(leaveType.getId())
                .accruedDays(new BigDecimal("1.00"))
                .carriedOverDays(new BigDecimal("0.00"))
                .usedDays(new BigDecimal("0.00"))
                .pendingDays(new BigDecimal("0.00"))
                .build());

        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            final long requestId = 9000L + i;
            futures.add(executor.submit(() -> {
                try {
                    startLatch.await();
                    leaveBalanceService.reserveBalance(user.getId(), leaveType.getId(), new BigDecimal("1.00"), user.getId(), requestId);
                    return true;
                } catch (Exception ex) {
                    return false;
                }
            }));
        }

        startLatch.countDown();

        int successes = 0;
        for (Future<Boolean> future : futures) {
            if (Boolean.TRUE.equals(future.get())) {
                successes++;
            }
        }

        executor.shutdown();

        assertThat(successes).isEqualTo(1);
    }
}
