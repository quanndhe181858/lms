package com.enterprise.lms.module.leave.service;

import com.enterprise.lms.module.leave.entity.LeaveRequest;
import com.enterprise.lms.module.leave.repository.LeaveRequestRepository;
import com.enterprise.lms.module.leave.repository.PublicHolidayRepository;
import com.enterprise.lms.module.user.entity.User;
import com.enterprise.lms.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SlaEscalationService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final UserRepository userRepository;
    private final PublicHolidayRepository publicHolidayRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public int processPendingSla() {
        List<LeaveRequest> pendingRequests = leaveRequestRepository.findByStatusOrderBySubmittedAtAsc(LeaveRequest.Status.SUBMITTED);
        int processed = 0;

        for (LeaveRequest request : pendingRequests) {
            if (request.getSubmittedAt() == null) {
                continue;
            }

            long businessMinutesElapsed = calculateBusinessMinutesElapsed(request.getSubmittedAt(), LocalDateTime.now());

            if (!request.isReminderSent() && businessMinutesElapsed >= 24 * 60L) {
                request.setReminderSent(true);
                leaveRequestRepository.save(request);
                processed++;
            }

            if (businessMinutesElapsed >= 48 * 60L) {
                escalateRequest(request);
                processed++;
            }
        }

        return processed;
    }

    @Transactional
    public void escalateRequest(LeaveRequest request) {
        if (request == null || request.getStatus() != LeaveRequest.Status.SUBMITTED) {
            return;
        }

        LeaveRequest.Status previousStatus = request.getStatus();
        request.setStatus(LeaveRequest.Status.ESCALATED);
        request.setEscalatedAt(LocalDateTime.now());
        request.setReminderSent(true);

        if (request.getAssignedApproverId() != null) {
            User currentApprover = userRepository.findById(request.getAssignedApproverId()).orElse(null);
            if (currentApprover != null && currentApprover.getManager() != null) {
                request.setAssignedApproverId(currentApprover.getManager().getId());
            }
        }

        LeaveRequest saved = leaveRequestRepository.save(request);
        auditLogService.recordAsync(
                "LeaveRequest",
                saved.getId(),
                "ESCALATE",
                saved.getAssignedApproverId(),
                java.util.Map.of("status", previousStatus.name()),
                java.util.Map.of("status", saved.getStatus().name(), "escalatedAt", saved.getEscalatedAt().toString()),
                "system",
                java.util.Map.of("escalationReason", "SLA_EXCEEDED")
        );
    }

    long calculateBusinessMinutesElapsed(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null || !to.isAfter(from)) {
            return 0L;
        }

        long totalMinutes = 0L;
        LocalDate cursor = from.toLocalDate();
        LocalDate endDate = to.toLocalDate();

        while (!cursor.isAfter(endDate)) {
            if (!isWeekend(cursor) && !isPublicHoliday(cursor)) {
                LocalDateTime workStart = LocalDateTime.of(cursor, LocalTime.of(8, 0));
                LocalDateTime workEnd = LocalDateTime.of(cursor, LocalTime.of(17, 0));
                LocalDateTime intervalStart = max(from, workStart);
                LocalDateTime intervalEnd = min(to, workEnd);

                if (!intervalEnd.isAfter(intervalStart)) {
                    cursor = cursor.plusDays(1);
                    continue;
                }

                LocalDateTime lunchStart = LocalDateTime.of(cursor, LocalTime.of(12, 0));
                LocalDateTime lunchEnd = LocalDateTime.of(cursor, LocalTime.of(13, 0));

                long minutes = Duration.between(intervalStart, intervalEnd).toMinutes();
                long lunchMinutes = calculateOverlapMinutes(intervalStart, intervalEnd, lunchStart, lunchEnd);
                totalMinutes += minutes - lunchMinutes;
            }
            cursor = cursor.plusDays(1);
        }

        return totalMinutes;
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    private boolean isPublicHoliday(LocalDate date) {
        return publicHolidayRepository.findByHolidayDate(date).isPresent();
    }

    private long calculateOverlapMinutes(LocalDateTime intervalStart, LocalDateTime intervalEnd, LocalDateTime lunchStart, LocalDateTime lunchEnd) {
        if (intervalEnd.isBefore(lunchStart) || intervalStart.isAfter(lunchEnd)) {
            return 0L;
        }

        LocalDateTime overlapStart = max(intervalStart, lunchStart);
        LocalDateTime overlapEnd = min(intervalEnd, lunchEnd);
        return Duration.between(overlapStart, overlapEnd).toMinutes();
    }

    private LocalDateTime max(LocalDateTime left, LocalDateTime right) {
        return left.isAfter(right) ? left : right;
    }

    private LocalDateTime min(LocalDateTime left, LocalDateTime right) {
        return left.isBefore(right) ? left : right;
    }
}
