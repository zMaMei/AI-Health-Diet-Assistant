package com.health.diet.dto.vo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/* 推荐页数据返回 */
public class RecommendationPageVO {
    /* 推荐列表 */
    private List<RecommendationVO> recommendations;
    /* 营养素阈值 */
    private Map<String, BigDecimal> thresholds;

    public List<RecommendationVO> getRecommendations() { return recommendations; }
    public void setRecommendations(List<RecommendationVO> recommendations) { this.recommendations = recommendations; }
    public Map<String, BigDecimal> getThresholds() { return thresholds; }
    public void setThresholds(Map<String, BigDecimal> thresholds) { this.thresholds = thresholds; }
}
