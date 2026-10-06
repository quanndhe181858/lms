package com.enterprise.lms.module.leave.repository;

import com.enterprise.lms.module.leave.entity.PublicHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PublicHolidayRepository extends JpaRepository<PublicHoliday, Long> {
    Optional<PublicHoliday> findByHolidayDate(LocalDate holidayDate);
    List<PublicHoliday> findByCalendarYear(int calendarYear);
}
