package com.enterprise.lms.module.leave.repository;

import com.enterprise.lms.module.leave.entity.LeaveLedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveLedgerEntryRepository extends JpaRepository<LeaveLedgerEntry, Long> {
    List<LeaveLedgerEntry> findByUserIdAndLeaveTypeIdOrderByCreatedAtAsc(Long userId, Long leaveTypeId);
}
