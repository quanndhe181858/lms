package com.enterprise.lms.module.leave.entity;

import com.enterprise.lms.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(
        name = "leave_balances",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_leave_type", columnNames = {"user_id", "leave_type_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveBalance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "leave_type_id", nullable = false)
    private Long leaveTypeId;

    @Column(name = "accrued_days", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal accruedDays = BigDecimal.ZERO;

    @Column(name = "carried_over_days", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal carriedOverDays = BigDecimal.ZERO;

    @Column(name = "used_days", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal usedDays = BigDecimal.ZERO;

    @Column(name = "pending_days", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal pendingDays = BigDecimal.ZERO;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    public BigDecimal calculateAvailable() {
        return accruedDays.add(carriedOverDays).subtract(usedDays).subtract(pendingDays);
    }
}
