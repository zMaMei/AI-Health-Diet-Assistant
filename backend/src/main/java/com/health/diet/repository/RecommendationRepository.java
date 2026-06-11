package com.health.diet.repository;

import com.health.diet.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    List<Recommendation> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Recommendation> findByUserIdAndFeedbackIsNull(Long userId);
}
