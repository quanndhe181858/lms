package com.enterprise.lms.module.leave.repository;

import com.enterprise.lms.module.leave.entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    Optional<LeaveRequest> findByIdempotencyKey(String idempotencyKey);

    List<LeaveRequest> findByStatusOrderBySubmittedAtAsc(LeaveRequest.Status status);

    List<LeaveRequest> findByUserIdOrderBySubmittedAtDesc(Long userId);

    List<LeaveRequest> findByAssignedApproverIdAndStatusIn(Long assignedApproverId, Collection<LeaveRequest.Status> statuses);

    List<LeaveRequest> findByStatusIn(Collection<LeaveRequest.Status> statuses);

    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM LeaveRequest l " +
            "WHERE l.userId = :userId AND l.status IN :statuses " +
            "AND l.startDate <= :endDate AND l.endDate >= :startDate")
    boolean existsByUserIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            @Param("userId") Long userId,
            @Param("statuses") Collection<LeaveRequest.Status> statuses,
            @Param("endDate") LocalDate endDate,
            @Param("startDate") LocalDate startDate
    );

    @Query("SELECT l FROM LeaveRequest l WHERE l.userId IN :userIds AND l.status IN :statuses " +
            "AND l.startDate <= :endDate AND l.endDate >= :startDate")
    List<LeaveRequest> findTeamSchedule(
            @Param("userIds") Collection<Long> userIds,
            @Param("statuses") Collection<LeaveRequest.Status> statuses,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
