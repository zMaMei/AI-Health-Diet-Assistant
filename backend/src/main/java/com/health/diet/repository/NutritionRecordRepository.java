package com.health.diet.repository;

import com.health.diet.entity.NutritionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/* 每日营养汇总数据访问层 */
public interface NutritionRecordRepository extends JpaRepository<NutritionRecord, Long> {

    /* findByUserIdAndRecordDate 按用户和日期查询营养汇总 */
    Optional<NutritionRecord> findByUserIdAndRecordDate(Long userId, LocalDate recordDate);

    /* findByUserIdAndRecordDateBetweenOrderByRecordDateAsc 按用户和日期区间查询营养汇总列表 */
    List<NutritionRecord> findByUserIdAndRecordDateBetweenOrderByRecordDateAsc(Long userId, LocalDate start, LocalDate end);
}
