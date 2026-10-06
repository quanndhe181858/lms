package com.enterprise.lms.module.leave.dto;

import java.math.BigDecimal;

public record PayrollExportRow(
        Long employeeId,
        String employeeName,
        String department,
        BigDecimal unpaidLeaveDays,
        BigDecimal paidLeaveDaysTaken,
        BigDecimal closingBalance
) {
}
