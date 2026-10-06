package com.enterprise.lms.module.leave;

import com.enterprise.lms.module.leave.entity.LeaveBalance;
import com.enterprise.lms.module.leave.entity.LeaveLedgerEntry;
import com.enterprise.lms.module.leave.entity.LeaveType;
import com.enterprise.lms.module.leave.repository.LeaveBalanceRepository;
import com.enterprise.lms.module.leave.repository.LeaveLedgerEntryRepository;
import com.enterprise.lms.module.leave.repository.LeaveTypeRepository;
import com.enterprise.lms.module.user.entity.Department;
import com.enterprise.lms.module.user.entity.Role;
import com.enterprise.lms.module.user.entity.User;
import com.enterprise.lms.module.user.repository.DepartmentRepository;
import com.enterprise.lms.module.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class PayrollReportControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
    void hrAdmin_canDownloadCsvPayrollReport() throws Exception {
        Department department = departmentRepository.findByDepartmentCode("ENG").orElseThrow();
        User manager = userRepository.findByEmail("david.manager@lms.local").orElseThrow();

        LeaveType annualLeave = leaveTypeRepository.findByCode("ANNUAL")
                .orElseGet(() -> leaveTypeRepository.save(LeaveType.builder()
                        .code("ANNUAL")
                        .name("Annual Leave")
                        .defaultDaysPerYear(new BigDecimal("20.00"))
                        .isActive(true)
                        .build()));

        User employee = userRepository.save(User.builder()
                .email("payroll.employee@lms.local")
                .passwordHash("hash")
                .fullName("Payroll Employee")
                .department(department)
                .manager(manager)
                .role(Role.ROLE_EMPLOYEE)
                .employmentStatus(com.enterprise.lms.module.user.entity.EmploymentStatus.PERMANENT)
                .hireDate(LocalDate.of(2024, 1, 1))
                .active(true)
                .build());

        leaveBalanceRepository.save(LeaveBalance.builder()
                .userId(employee.getId())
                .leaveTypeId(annualLeave.getId())
                .accruedDays(new BigDecimal("2.00"))
                .carriedOverDays(BigDecimal.ZERO)
                .usedDays(new BigDecimal("1.50"))
                .pendingDays(BigDecimal.ZERO)
                .build());

        leaveLedgerEntryRepository.save(LeaveLedgerEntry.builder()
                .transactionUuid(UUID.randomUUID().toString())
                .userId(employee.getId())
                .leaveTypeId(annualLeave.getId())
                .requestId(null)
                .amount(new BigDecimal("-1.50"))
                .entryType(LeaveLedgerEntry.EntryType.DEDUCTION)
                .balanceAfter(new BigDecimal("0.50"))
                .description("Approved leave deduction")
                .actorUserId(manager.getId())
                .build());

        mockMvc.perform(get("/api/v1/reports/payroll")
                        .param("month", "2026-09")
                        .param("format", "csv")
                        .with(user("admin@lms.local").roles("HR_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("payroll-2026-09.csv")))
                .andExpect(content().string(containsString("EmployeeID")))
                .andExpect(content().string(containsString("UnpaidLeaveDays")))
                .andExpect(content().string(containsString("ClosingBalance")));
    }

    @Test
    void employee_isForbiddenFromPayrollReport() throws Exception {
        mockMvc.perform(get("/api/v1/reports/payroll")
                        .param("month", "2026-09")
                        .param("format", "csv")
                        .with(user("sarah.engineer@lms.local").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
    }
}
