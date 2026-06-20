package com.health.diet.dto.vo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class RecommendationPageVO {
    private List<RecommendationVO> recommendations;
    private Map<String, BigDecimal> thresholds;

    public List<RecommendationVO> getRecommendations() { return recommendations; }
    public void setRecommendations(List<RecommendationVO> recommendations) { this.recommendations = recommendations; }
    public Map<String, BigDecimal> getThresholds() { return thresholds; }
    public void setThresholds(Map<String, BigDecimal> thresholds) { this.thresholds = thresholds; }
}
