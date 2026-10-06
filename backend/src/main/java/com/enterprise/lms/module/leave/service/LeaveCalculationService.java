package com.enterprise.lms.module.leave.service;

import com.enterprise.lms.module.leave.entity.PublicHoliday;
import com.enterprise.lms.module.leave.repository.PublicHolidayRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
public class LeaveCalculationService {

    private final PublicHolidayRepository publicHolidayRepository;

    public LeaveCalculationService(PublicHolidayRepository publicHolidayRepository) {
        this.publicHolidayRepository = publicHolidayRepository;
    }

    public BigDecimal calculateBillableDays(LocalDate startDate, LocalDate endDate, HalfDay startHalfDay, HalfDay endHalfDay) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start and end dates are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }

        if (startDate.equals(endDate)) {
            return calculateSingleDayBillableHours(startDate, startHalfDay, endHalfDay);
        }

        BigDecimal total = BigDecimal.ZERO;
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (isWeekend(date) || isPublicHoliday(date)) {
                continue;
            }

            total = total.add(BigDecimal.ONE);
        }

        return total;
    }

    public boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    public boolean isPublicHoliday(LocalDate date) {
        return publicHolidayRepository.findByHolidayDate(date).isPresent();
    }

    private BigDecimal calculateSingleDayBillableHours(LocalDate date, HalfDay startHalfDay, HalfDay endHalfDay) {
        if (isWeekend(date) || isPublicHoliday(date)) {
            return BigDecimal.ZERO;
        }

        if (startHalfDay == null || endHalfDay == null) {
            return BigDecimal.ONE;
        }

        if (startHalfDay == endHalfDay) {
            return new BigDecimal("0.5");
        }

        return BigDecimal.ONE;
    }

    public enum HalfDay {
        MORNING,
        AFTERNOON
    }
}
