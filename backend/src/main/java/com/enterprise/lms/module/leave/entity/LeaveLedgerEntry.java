package com.enterprise.lms.module.leave.entity;

import com.enterprise.lms.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "leave_ledger_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveLedgerEntry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_uuid", nullable = false, unique = true, length = 64)
    private String transactionUuid;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "leave_type_id", nullable = false)
    private Long leaveTypeId;

    @Column(name = "request_id")
    private Long requestId;

    @Column(name = "amount", nullable = false, precision = 5, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 30)
    private EntryType entryType;

    @Column(name = "balance_after", nullable = false, precision = 5, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(name = "actor_user_id", nullable = false)
    private Long actorUserId;

    public enum EntryType {
        ACCRUAL,
        RESERVATION,
        RESERVATION_RELEASE,
        DEDUCTION,
        REVERSAL,
        EXPIRATION_FORFEIT,
        MANUAL_ADJUSTMENT
    }
}
