package com.enterprise.lms.module.leave.controller;

import com.enterprise.lms.module.leave.service.PayrollExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PayrollReportController {

    private final PayrollExportService payrollExportService;

    @GetMapping("/reports/payroll")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<byte[]> exportPayroll(
            @RequestParam String month,
            @RequestParam(defaultValue = "csv") String format
    ) {
        if (month == null || month.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MONTH_REQUIRED");
        }

        try {
            YearMonth.parse(month);
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_MONTH_FORMAT");
        }

        String normalizedFormat = format == null ? "csv" : format.trim().toLowerCase();
        byte[] payload;
        String fileName;
        MediaType mediaType;

        if ("xlsx".equals(normalizedFormat)) {
            payload = payrollExportService.generateXlsx(month);
            fileName = "payroll-" + month + ".xlsx";
            mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        } else {
            payload = payrollExportService.generateCsv(month);
            fileName = "payroll-" + month + ".csv";
            mediaType = MediaType.parseMediaType("text/csv");
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(payload);
    }
}
