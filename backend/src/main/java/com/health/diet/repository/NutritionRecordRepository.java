package com.health.diet.repository;

import com.health.diet.entity.NutritionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface NutritionRecordRepository extends JpaRepository<NutritionRecord, Long> {
    Optional<NutritionRecord> findByUserIdAndRecordDate(Long userId, LocalDate recordDate);
    List<NutritionRecord> findByUserIdAndRecordDateBetweenOrderByRecordDateAsc(Long userId, LocalDate start, LocalDate end);
}
