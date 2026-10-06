package com.enterprise.lms.module.leave.dto;

import com.enterprise.lms.module.leave.service.ApprovalService;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApprovalDecisionRequest {

    @NotNull(message = "decision is required")
    private ApprovalService.Decision decision;

    private String decisionReason;
}
