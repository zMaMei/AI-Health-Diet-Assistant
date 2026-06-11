package com.health.diet.repository;

import com.health.diet.entity.DietRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface DietRecordRepository extends JpaRepository<DietRecord, Long> {
    List<DietRecord> findByUserIdAndRecordTimeBetweenOrderByRecordTimeAsc(Long userId, LocalDateTime start, LocalDateTime end);

    default List<DietRecord> findByUserAndDate(Long userId, LocalDate date) {
        return findByUserIdAndRecordTimeBetweenOrderByRecordTimeAsc(
                userId, date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }
}
