package com.enterprise.lms.module.leave.scheduler;

import com.enterprise.lms.module.leave.service.SlaEscalationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SlaEscalationScheduler {

    private final SlaEscalationService slaEscalationService;

    @Scheduled(cron = "0 */15 * * * ?")
    public void processSlaQueue() {
        slaEscalationService.processPendingSla();
    }
}
