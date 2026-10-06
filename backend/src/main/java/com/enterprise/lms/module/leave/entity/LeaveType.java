package com.enterprise.lms.module.leave.entity;

import com.enterprise.lms.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "leave_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveType extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "default_days_per_year", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal defaultDaysPerYear = BigDecimal.ZERO;

    @Column(name = "is_confidential_attachment", nullable = false)
    @Builder.Default
    private boolean isConfidentialAttachment = false;

    @Column(name = "requires_attachment_days", nullable = false)
    @Builder.Default
    private int requiresAttachmentDays = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
