package com.health.diet.repository;

import com.health.diet.entity.MealPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MealPhotoRepository extends JpaRepository<MealPhoto, Long> {

    List<MealPhoto> findByUserIdAndRecordDateOrderByCreatedAtAsc(Long userId, LocalDate recordDate);

    List<MealPhoto> findByUserIdAndRecordDateAndMealTypeOrderByCreatedAtAsc(
            Long userId, LocalDate recordDate, String mealType);

    void deleteByUserIdAndRecordDateAndMealType(Long userId, LocalDate recordDate, String mealType);
}
