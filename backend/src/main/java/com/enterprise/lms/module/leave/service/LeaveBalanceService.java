package com.enterprise.lms.module.leave.service;

import com.enterprise.lms.module.leave.entity.LeaveBalance;
import com.enterprise.lms.module.leave.entity.LeaveLedgerEntry;
import com.enterprise.lms.module.leave.repository.LeaveBalanceRepository;
import com.enterprise.lms.module.leave.repository.LeaveLedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeaveBalanceService {

    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveLedgerEntryRepository leaveLedgerEntryRepository;

    @Transactional
    public BigDecimal reserveBalance(Long userId, Long leaveTypeId, BigDecimal requestedDays, Long actorUserId, Long requestId) {
        validateRequestedDays(requestedDays);

        if (Long.valueOf(3L).equals(leaveTypeId)) {
            return BigDecimal.ZERO;
        }

        LeaveBalance balance = leaveBalanceRepository.findByUserIdAndLeaveTypeIdForUpdate(userId, leaveTypeId)
                .orElseGet(() -> leaveBalanceRepository.save(LeaveBalance.builder()
                        .userId(userId)
                        .leaveTypeId(leaveTypeId)
                        .accruedDays(BigDecimal.ZERO)
                        .carriedOverDays(BigDecimal.ZERO)
                        .usedDays(BigDecimal.ZERO)
                        .pendingDays(BigDecimal.ZERO)
                        .build()));

        BigDecimal availableBefore = balance.calculateAvailable();
        if (availableBefore.compareTo(requestedDays) < 0) {
            throw new IllegalArgumentException("INSUFFICIENT_BALANCE");
        }

        BigDecimal newPending = balance.getPendingDays().add(requestedDays);
        balance.setPendingDays(newPending);

        BigDecimal balanceAfter = balance.calculateAvailable();
        leaveBalanceRepository.save(balance);
        leaveLedgerEntryRepository.save(LeaveLedgerEntry.builder()
                .transactionUuid(UUID.randomUUID().toString())
                .userId(userId)
                .leaveTypeId(leaveTypeId)
                .requestId(requestId)
                .amount(requestedDays.negate())
                .entryType(LeaveLedgerEntry.EntryType.RESERVATION)
                .balanceAfter(balanceAfter)
                .description("Leave request reserved")
                .actorUserId(actorUserId)
                .build());

        return balanceAfter;
    }

    @Transactional
    public BigDecimal releaseReservation(Long userId, Long leaveTypeId, BigDecimal releasedDays, Long actorUserId, Long requestId) {
        validateRequestedDays(releasedDays);

        if (Long.valueOf(3L).equals(leaveTypeId)) {
            return BigDecimal.ZERO;
        }

        LeaveBalance balance = leaveBalanceRepository.findByUserIdAndLeaveTypeIdForUpdate(userId, leaveTypeId)
                .orElseThrow(() -> new IllegalArgumentException("BALANCE_NOT_FOUND"));

        if (balance.getPendingDays().compareTo(releasedDays) < 0) {
            throw new IllegalArgumentException("INVALID_RELEASE_AMOUNT");
        }

        balance.setPendingDays(balance.getPendingDays().subtract(releasedDays));
        BigDecimal balanceAfter = balance.calculateAvailable();
        leaveBalanceRepository.save(balance);
        leaveLedgerEntryRepository.save(LeaveLedgerEntry.builder()
                .transactionUuid(UUID.randomUUID().toString())
                .userId(userId)
                .leaveTypeId(leaveTypeId)
                .requestId(requestId)
                .amount(releasedDays)
                .entryType(LeaveLedgerEntry.EntryType.RESERVATION_RELEASE)
                .balanceAfter(balanceAfter)
                .description("Reservation released")
                .actorUserId(actorUserId)
                .build());

        return balanceAfter;
    }

    @Transactional
    public BigDecimal finalizeApprovedReservation(Long userId, Long leaveTypeId, BigDecimal approvedDays, Long actorUserId, Long requestId) {
        validateRequestedDays(approvedDays);

        if (Long.valueOf(3L).equals(leaveTypeId)) {
            return BigDecimal.ZERO;
        }

        LeaveBalance balance = leaveBalanceRepository.findByUserIdAndLeaveTypeIdForUpdate(userId, leaveTypeId)
                .orElseThrow(() -> new IllegalArgumentException("BALANCE_NOT_FOUND"));

        if (balance.getPendingDays().compareTo(approvedDays) < 0) {
            throw new IllegalArgumentException("INVALID_APPROVAL_AMOUNT");
        }

        balance.setPendingDays(balance.getPendingDays().subtract(approvedDays));
        balance.setUsedDays(balance.getUsedDays().add(approvedDays));
        BigDecimal balanceAfter = balance.calculateAvailable();
        leaveBalanceRepository.save(balance);
        leaveLedgerEntryRepository.save(LeaveLedgerEntry.builder()
                .transactionUuid(UUID.randomUUID().toString())
                .userId(userId)
                .leaveTypeId(leaveTypeId)
                .requestId(requestId)
                .amount(approvedDays.negate())
                .entryType(LeaveLedgerEntry.EntryType.DEDUCTION)
                .balanceAfter(balanceAfter)
                .description("Approved leave deducted from balance")
                .actorUserId(actorUserId)
                .build());

        return balanceAfter;
    }

    @Transactional
    public BigDecimal reverseUsedDays(Long userId, Long leaveTypeId, BigDecimal daysToReverse, Long actorUserId, Long requestId) {
        validateRequestedDays(daysToReverse);

        LeaveBalance balance = leaveBalanceRepository.findByUserIdAndLeaveTypeIdForUpdate(userId, leaveTypeId)
                .orElseThrow(() -> new IllegalArgumentException("BALANCE_NOT_FOUND"));

        if (balance.getUsedDays().compareTo(daysToReverse) < 0) {
            throw new IllegalArgumentException("INVALID_REVERSAL_AMOUNT");
        }

        balance.setUsedDays(balance.getUsedDays().subtract(daysToReverse));
        BigDecimal balanceAfter = balance.calculateAvailable();
        leaveBalanceRepository.save(balance);
        leaveLedgerEntryRepository.save(LeaveLedgerEntry.builder()
                .transactionUuid(UUID.randomUUID().toString())
                .userId(userId)
                .leaveTypeId(leaveTypeId)
                .requestId(requestId)
                .amount(daysToReverse)
                .entryType(LeaveLedgerEntry.EntryType.REVERSAL)
                .balanceAfter(balanceAfter)
                .description("Cancelled leave reversed into available balance")
                .actorUserId(actorUserId)
                .build());

        return balanceAfter;
    }

    private void validateRequestedDays(BigDecimal requestedDays) {
        if (requestedDays == null || requestedDays.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("INVALID_REQUESTED_DAYS");
        }
    }
}
