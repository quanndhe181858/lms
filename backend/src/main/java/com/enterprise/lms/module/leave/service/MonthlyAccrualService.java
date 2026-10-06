package com.enterprise.lms.module.leave.service;

import com.enterprise.lms.module.leave.entity.LeaveBalance;
import com.enterprise.lms.module.leave.entity.LeaveLedgerEntry;
import com.enterprise.lms.module.leave.entity.LeaveType;
import com.enterprise.lms.module.leave.repository.LeaveBalanceRepository;
import com.enterprise.lms.module.leave.repository.LeaveLedgerEntryRepository;
import com.enterprise.lms.module.leave.repository.LeaveTypeRepository;
import com.enterprise.lms.module.user.entity.EmploymentStatus;
import com.enterprise.lms.module.user.entity.User;
import com.enterprise.lms.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MonthlyAccrualService {

    private static final String ANNUAL_LEAVE_CODE = "ANNUAL";
    private static final BigDecimal PROBATION_MONTHLY_ACCRUAL = new BigDecimal("1.00");
    private static final BigDecimal PERMANENT_MONTHLY_ACCRUAL = new BigDecimal("20.00")
            .divide(new BigDecimal("12"), 4, RoundingMode.DOWN)
            .setScale(2, RoundingMode.DOWN);

    private final UserRepository userRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveLedgerEntryRepository leaveLedgerEntryRepository;

    @Transactional
    public int runMonthlyAccrual() {
        LeaveType annualLeaveType = leaveTypeRepository.findByCode(ANNUAL_LEAVE_CODE)
                .orElseGet(() -> leaveTypeRepository.save(LeaveType.builder()
                        .code(ANNUAL_LEAVE_CODE)
                        .name("Annual Leave")
                        .defaultDaysPerYear(new BigDecimal("20.00"))
                        .isActive(true)
                        .build()));

        List<User> activeUsers = userRepository.findByActiveTrue();
        int accrualCount = 0;

        for (User user : activeUsers) {
            if (user.getEmploymentStatus() == null) {
                continue;
            }

            BigDecimal accrualAmount = resolveMonthlyAccrual(user.getEmploymentStatus());
            if (accrualAmount.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            LeaveBalance balance = leaveBalanceRepository.findByUserIdAndLeaveTypeId(user.getId(), annualLeaveType.getId())
                    .orElseGet(() -> leaveBalanceRepository.save(LeaveBalance.builder()
                            .userId(user.getId())
                            .leaveTypeId(annualLeaveType.getId())
                            .accruedDays(BigDecimal.ZERO)
                            .carriedOverDays(BigDecimal.ZERO)
                            .usedDays(BigDecimal.ZERO)
                            .pendingDays(BigDecimal.ZERO)
                            .build()));

            balance.setAccruedDays(balance.getAccruedDays().add(accrualAmount));
            leaveBalanceRepository.save(balance);

            leaveLedgerEntryRepository.save(LeaveLedgerEntry.builder()
                    .transactionUuid(UUID.randomUUID().toString())
                    .userId(user.getId())
                    .leaveTypeId(annualLeaveType.getId())
                    .amount(accrualAmount)
                    .entryType(LeaveLedgerEntry.EntryType.ACCRUAL)
                    .balanceAfter(balance.calculateAvailable())
                    .description("Monthly accrual")
                    .actorUserId(user.getId())
                    .build());

            accrualCount++;
        }

        return accrualCount;
    }

    private BigDecimal resolveMonthlyAccrual(EmploymentStatus employmentStatus) {
        if (employmentStatus == EmploymentStatus.PROBATION) {
            return PROBATION_MONTHLY_ACCRUAL;
        }
        if (employmentStatus == EmploymentStatus.PERMANENT) {
            return PERMANENT_MONTHLY_ACCRUAL;
        }
        return BigDecimal.ZERO;
    }
}
