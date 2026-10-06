package com.enterprise.lms.module.leave;

import com.enterprise.lms.module.leave.entity.LeaveBalance;
import com.enterprise.lms.module.leave.entity.LeaveLedgerEntry;
import com.enterprise.lms.module.leave.entity.LeaveType;
import com.enterprise.lms.module.leave.repository.LeaveBalanceRepository;
import com.enterprise.lms.module.leave.repository.LeaveLedgerEntryRepository;
import com.enterprise.lms.module.leave.repository.LeaveTypeRepository;
import com.enterprise.lms.module.leave.service.MonthlyAccrualService;
import com.enterprise.lms.module.user.entity.Department;
import com.enterprise.lms.module.user.entity.EmploymentStatus;
import com.enterprise.lms.module.user.entity.Role;
import com.enterprise.lms.module.user.entity.User;
import com.enterprise.lms.module.user.repository.DepartmentRepository;
import com.enterprise.lms.module.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
class MonthlyAccrualServiceIntegrationTest {

    @Autowired
    private MonthlyAccrualService monthlyAccrualService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    private LeaveLedgerEntryRepository leaveLedgerEntryRepository;

    @Test
    void runMonthlyAccrual_creditsProbationAndPermanentEmployees() {
        Department department = departmentRepository.findByDepartmentCode("ENG").orElseThrow();
        User manager = userRepository.findByEmail("david.manager@lms.local").orElseThrow();

        LeaveType annualLeave = leaveTypeRepository.findByCode("ANNUAL")
                .orElseGet(() -> leaveTypeRepository.save(LeaveType.builder()
                        .code("ANNUAL")
                        .name("Annual Leave")
                        .defaultDaysPerYear(new BigDecimal("20.00"))
                        .isActive(true)
                        .build()));

        User probationEmployee = userRepository.save(User.builder()
                .email("probation.accrual@lms.local")
                .passwordHash("hash")
                .fullName("Probation Employee")
                .department(department)
                .manager(manager)
                .role(Role.ROLE_EMPLOYEE)
                .employmentStatus(EmploymentStatus.PROBATION)
                .hireDate(LocalDate.of(2026, 9, 1))
                .active(true)
                .build());

        User permanentEmployee = userRepository.save(User.builder()
                .email("permanent.accrual@lms.local")
                .passwordHash("hash")
                .fullName("Permanent Employee")
                .department(department)
                .manager(manager)
                .role(Role.ROLE_EMPLOYEE)
                .employmentStatus(EmploymentStatus.PERMANENT)
                .hireDate(LocalDate.of(2024, 6, 1))
                .active(true)
                .build());

        monthlyAccrualService.runMonthlyAccrual();

        LeaveBalance probationBalance = leaveBalanceRepository.findByUserIdAndLeaveTypeId(probationEmployee.getId(), annualLeave.getId())
                .orElseThrow();
        LeaveBalance permanentBalance = leaveBalanceRepository.findByUserIdAndLeaveTypeId(permanentEmployee.getId(), annualLeave.getId())
                .orElseThrow();

        assertThat(probationBalance.getAccruedDays()).isEqualByComparingTo("1.00");
        assertThat(permanentBalance.getAccruedDays()).isEqualByComparingTo("1.66");

        assertThat(leaveLedgerEntryRepository.findByUserIdAndLeaveTypeIdOrderByCreatedAtAsc(
                probationEmployee.getId(), annualLeave.getId()))
                .extracting(LeaveLedgerEntry::getEntryType)
                .containsExactly(LeaveLedgerEntry.EntryType.ACCRUAL);

        assertThat(leaveLedgerEntryRepository.findByUserIdAndLeaveTypeIdOrderByCreatedAtAsc(
                permanentEmployee.getId(), annualLeave.getId()))
                .extracting(LeaveLedgerEntry::getEntryType)
                .containsExactly(LeaveLedgerEntry.EntryType.ACCRUAL);
    }
}
