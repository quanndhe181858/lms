package com.enterprise.lms.module.leave.scheduler;

import com.enterprise.lms.module.leave.service.MonthlyAccrualService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MonthlyAccrualScheduler {

    private final MonthlyAccrualService monthlyAccrualService;

    @Scheduled(cron = "0 5 0 1 * ?")
    public void processMonthlyAccrual() {
        monthlyAccrualService.runMonthlyAccrual();
    }
}
