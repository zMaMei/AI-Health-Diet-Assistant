package com.health.diet.repository;

import com.health.diet.entity.MealPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

/* 餐次照片数据访问层 */
public interface MealPhotoRepository extends JpaRepository<MealPhoto, Long> {

    /* findByUserIdAndRecordDateOrderByCreatedAtAsc 按用户和日期查询照片列表 */
    List<MealPhoto> findByUserIdAndRecordDateOrderByCreatedAtAsc(Long userId, LocalDate recordDate);

    /* findByUserIdAndRecordDateAndMealTypeOrderByCreatedAtAsc 按用户、日期和餐次类型查询照片 */
    List<MealPhoto> findByUserIdAndRecordDateAndMealTypeOrderByCreatedAtAsc(
            Long userId, LocalDate recordDate, String mealType);

    /* deleteByUserIdAndRecordDateAndMealType 按用户、日期和餐次类型删除照片 */
    void deleteByUserIdAndRecordDateAndMealType(Long userId, LocalDate recordDate, String mealType);
}
