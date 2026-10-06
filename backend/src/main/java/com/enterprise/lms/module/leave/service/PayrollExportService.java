package com.enterprise.lms.module.leave.service;

import com.enterprise.lms.module.leave.dto.PayrollExportRow;
import com.enterprise.lms.module.leave.entity.LeaveBalance;
import com.enterprise.lms.module.leave.entity.LeaveLedgerEntry;
import com.enterprise.lms.module.leave.repository.LeaveBalanceRepository;
import com.enterprise.lms.module.leave.repository.LeaveLedgerEntryRepository;
import com.enterprise.lms.module.user.entity.User;
import com.enterprise.lms.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PayrollExportService {

    private final UserRepository userRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveLedgerEntryRepository leaveLedgerEntryRepository;

    public List<PayrollExportRow> getPayrollRowsForMonth(String month) {
        YearMonth targetMonth = YearMonth.parse(month);

        return userRepository.findByActiveTrue().stream()
                .filter(user -> user.getDepartment() != null)
                .map(user -> new PayrollExportRow(
                        user.getId(),
                        user.getFullName(),
                        user.getDepartment().getName(),
                        computeUnpaidLeaveDays(user.getId(), targetMonth),
                        computePaidLeaveDaysTaken(user.getId(), targetMonth),
                        computeClosingBalance(user.getId())
                ))
                .sorted(Comparator.comparing(PayrollExportRow::employeeId))
                .toList();
    }

    public byte[] generateCsv(String month) {
        List<PayrollExportRow> rows = getPayrollRowsForMonth(month);
        StringBuilder csv = new StringBuilder();
        csv.append("EmployeeID,EmployeeName,Department,UnpaidLeaveDays,PaidLeaveDaysTaken,ClosingBalance\n");

        for (PayrollExportRow row : rows) {
            csv.append(formatCsv(row.employeeId()))
                    .append(',')
                    .append(formatCsv(row.employeeName()))
                    .append(',')
                    .append(formatCsv(row.department()))
                    .append(',')
                    .append(formatDecimal(row.unpaidLeaveDays()))
                    .append(',')
                    .append(formatDecimal(row.paidLeaveDaysTaken()))
                    .append(',')
                    .append(formatDecimal(row.closingBalance()))
                    .append('\n');
        }

        return csv.toString().getBytes();
    }

    public byte[] generateXlsx(String month) {
        List<PayrollExportRow> rows = getPayrollRowsForMonth(month);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Payroll");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("EmployeeID");
            header.createCell(1).setCellValue("EmployeeName");
            header.createCell(2).setCellValue("Department");
            header.createCell(3).setCellValue("UnpaidLeaveDays");
            header.createCell(4).setCellValue("PaidLeaveDaysTaken");
            header.createCell(5).setCellValue("ClosingBalance");

            int rowIndex = 1;
            for (PayrollExportRow row : rows) {
                Row dataRow = sheet.createRow(rowIndex++);
                dataRow.createCell(0).setCellValue(row.employeeId());
                dataRow.createCell(1).setCellValue(row.employeeName());
                dataRow.createCell(2).setCellValue(row.department());
                dataRow.createCell(3).setCellValue(formatDecimal(row.unpaidLeaveDays()));
                dataRow.createCell(4).setCellValue(formatDecimal(row.paidLeaveDaysTaken()));
                dataRow.createCell(5).setCellValue(formatDecimal(row.closingBalance()));
            }

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to build payroll workbook", e);
        }
    }

    private BigDecimal computeUnpaidLeaveDays(Long userId, YearMonth targetMonth) {
        return leaveLedgerEntryRepository.findAll().stream()
                .filter(entry -> entry.getUserId().equals(userId))
                .filter(entry -> entry.getCreatedAt() != null)
                .filter(entry -> YearMonth.from(entry.getCreatedAt().toLocalDate()).equals(targetMonth))
                .filter(entry -> entry.getEntryType() == LeaveLedgerEntry.EntryType.DEDUCTION)
                .map(entry -> entry.getAmount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal computePaidLeaveDaysTaken(Long userId, YearMonth targetMonth) {
        return leaveLedgerEntryRepository.findAll().stream()
                .filter(entry -> entry.getUserId().equals(userId))
                .filter(entry -> entry.getCreatedAt() != null)
                .filter(entry -> YearMonth.from(entry.getCreatedAt().toLocalDate()).equals(targetMonth))
                .filter(entry -> entry.getEntryType() == LeaveLedgerEntry.EntryType.REVERSAL)
                .map(LeaveLedgerEntry::getAmount)
                .filter(amount -> amount.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal computeClosingBalance(Long userId) {
        return leaveBalanceRepository.findByUserIdOrderByLeaveTypeId(userId).stream()
                .map(LeaveBalance::calculateAvailable)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String formatDecimal(BigDecimal value) {
        if (value == null) {
            return "0.00";
        }
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatCsv(Object value) {
        if (value == null) {
            return "";
        }
        String raw = value.toString();
        if (raw.contains(",") || raw.contains("\"") || raw.contains("\n")) {
            return '"' + raw.replace("\"", "\"\"") + '"';
        }
        return raw;
    }
}
