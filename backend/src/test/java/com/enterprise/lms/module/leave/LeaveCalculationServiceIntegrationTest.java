package com.enterprise.lms.module.leave;

import com.enterprise.lms.module.leave.entity.PublicHoliday;
import com.enterprise.lms.module.leave.repository.PublicHolidayRepository;
import com.enterprise.lms.module.leave.service.LeaveCalculationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
class LeaveCalculationServiceIntegrationTest {

    @Autowired
    private LeaveCalculationService leaveCalculationService;

    @Autowired
    private PublicHolidayRepository publicHolidayRepository;

    @Test
    @DisplayName("Story 2.2: morning half-day is exactly 0.5 days")
    void calculateBillableDays_halfDayMorning_returnsHalfDay() {
        BigDecimal days = leaveCalculationService.calculateBillableDays(
                LocalDate.of(2026, 9, 14),
                LocalDate.of(2026, 9, 14),
                LeaveCalculationService.HalfDay.MORNING,
                LeaveCalculationService.HalfDay.MORNING
        );

        assertThat(days).isEqualByComparingTo("0.50");
    }

    @Test
    @DisplayName("Story 2.2: Friday to Monday with Monday public holiday deducts exactly one working day")
    void calculateBillableDays_withWeekendAndHoliday_skipsNonWorkingDays() {
        publicHolidayRepository.save(PublicHoliday.builder()
                .holidayDate(LocalDate.of(2026, 9, 14))
                .name("Public Holiday")
                .calendarYear(2026)
                .build());

        BigDecimal days = leaveCalculationService.calculateBillableDays(
                LocalDate.of(2026, 9, 11),
                LocalDate.of(2026, 9, 14),
                LeaveCalculationService.HalfDay.MORNING,
                LeaveCalculationService.HalfDay.AFTERNOON
        );

        assertThat(days).isEqualByComparingTo("1.00");
    }
}
