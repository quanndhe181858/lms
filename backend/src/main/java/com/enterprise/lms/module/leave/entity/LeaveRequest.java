package com.enterprise.lms.module.leave.entity;

import com.enterprise.lms.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "leave_requests",
        indexes = {
                @Index(name = "idx_requests_user_status", columnList = "user_id, status"),
                @Index(name = "idx_requests_approver_status", columnList = "assigned_approver_id, status"),
                @Index(name = "idx_requests_sla_lookup", columnList = "status, submitted_at, reminder_sent")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_uuid", nullable = false, unique = true, length = 64)
    private String requestUuid;

    @Column(name = "idempotency_key", length = 64, unique = true)
    private String idempotencyKey;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "leave_type_id", nullable = false)
    private Long leaveTypeId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "start_half", nullable = false, length = 20)
    @Builder.Default
    private HalfDay startHalf = HalfDay.MORNING;

    @Enumerated(EnumType.STRING)
    @Column(name = "end_half", nullable = false, length = 20)
    @Builder.Default
    private HalfDay endHalf = HalfDay.AFTERNOON;

    @Column(name = "total_billable_days", nullable = false, precision = 4, scale = 2)
    private BigDecimal totalBillableDays;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "attachment_id")
    private Long attachmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private Status status = Status.SUBMITTED;

    @Column(name = "assigned_approver_id")
    private Long assignedApproverId;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "is_backdated", nullable = false)
    @Builder.Default
    private boolean isBackdated = false;

    @Column(name = "reminder_sent", nullable = false)
    @Builder.Default
    private boolean reminderSent = false;

    @Column(name = "submitted_at", nullable = false)
    @Builder.Default
    private LocalDateTime submittedAt = LocalDateTime.now();

    @Column(name = "escalated_at")
    private LocalDateTime escalatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    public enum Status {
        DRAFT,
        SUBMITTED,
        ESCALATED,
        APPROVED,
        REJECTED,
        CANCELLED,
        CANCEL_REQUESTED
    }

    public enum HalfDay {
        MORNING,
        AFTERNOON
    }
}
