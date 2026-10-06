package com.enterprise.lms.module.leave.dto;

import com.enterprise.lms.module.leave.entity.LeaveRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class LeaveSubmitRequest {

    @NotNull(message = "leaveTypeId is required")
    private Long leaveTypeId;

    @NotNull(message = "startDate is required")
    private LocalDate startDate;

    @NotNull(message = "endDate is required")
    private LocalDate endDate;

    @NotNull(message = "startHalf is required")
    private LeaveRequest.HalfDay startHalf;

    @NotNull(message = "endHalf is required")
    private LeaveRequest.HalfDay endHalf;

    @NotBlank(message = "reason is required")
    private String reason;

    @NotNull(message = "totalBillableDays is required")
    private BigDecimal totalBillableDays;
}
