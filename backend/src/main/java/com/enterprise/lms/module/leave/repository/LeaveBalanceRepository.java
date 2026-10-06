package com.enterprise.lms.module.leave.repository;

import com.enterprise.lms.module.leave.entity.LeaveBalance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM LeaveBalance b WHERE b.userId = :userId AND b.leaveTypeId = :leaveTypeId")
    Optional<LeaveBalance> findByUserIdAndLeaveTypeIdForUpdate(
            @Param("userId") Long userId,
            @Param("leaveTypeId") Long leaveTypeId
    );

    Optional<LeaveBalance> findByUserIdAndLeaveTypeId(Long userId, Long leaveTypeId);

    List<LeaveBalance> findByUserIdOrderByLeaveTypeId(Long userId);
}
