package com.health.diet.repository;

import com.health.diet.entity.DietRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface DietRecordRepository extends JpaRepository<DietRecord, Long> {
    List<DietRecord> findByUserIdAndRecordTimeBetweenOrderByRecordTimeAsc(Long userId, LocalDateTime start, LocalDateTime end);

    default List<DietRecord> findByUserAndDate(Long userId, LocalDate date) {
        return findByUserIdAndRecordTimeBetweenOrderByRecordTimeAsc(
                userId, date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }

    /** 汇总某用户某日的营养合计 */
    @Query("SELECT COALESCE(SUM(d.calorie),0), COALESCE(SUM(d.protein),0), " +
           "COALESCE(SUM(d.fat),0), COALESCE(SUM(d.carbohydrate),0), " +
           "COALESCE(SUM(d.sugar),0), COALESCE(SUM(d.sodium),0) " +
           "FROM DietRecord d WHERE d.userId = :userId AND d.recordTime >= :start AND d.recordTime < :end")
    List<Object[]> sumNutritionRaw(@Param("userId") Long userId,
                                   @Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end);

    default BigDecimal[] sumNutrition(Long userId, LocalDate date) {
        List<Object[]> rows = sumNutritionRaw(userId, date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        if (rows.isEmpty() || rows.get(0) == null || rows.get(0).length == 0) {
            return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
        }
        Object[] row = rows.get(0);
        BigDecimal[] result = new BigDecimal[6];
        for (int i = 0; i < 6; i++) {
            result[i] = row[i] != null ? (BigDecimal) row[i] : BigDecimal.ZERO;
        }
        return result;
    }
}
