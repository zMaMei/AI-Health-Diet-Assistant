package com.health.diet.repository;

import com.health.diet.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/* 推荐记录数据访问层 */
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    /* findByUserIdOrderByCreatedAtDesc 按用户查询推荐记录列表（倒序） */
    List<Recommendation> findByUserIdOrderByCreatedAtDesc(Long userId);

    /* findByUserIdAndCreatedAtBetween 按用户和时间区间查询推荐记录 */
    List<Recommendation> findByUserIdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);

    /* deleteByUserIdAndCreatedAtBetween 按用户和时间区间删除推荐记录 */
    void deleteByUserIdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);
}
