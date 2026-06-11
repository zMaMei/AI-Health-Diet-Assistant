package com.health.diet.dto.command;

import jakarta.validation.constraints.NotNull;

public class RecommendationFeedbackCommand {

    @NotNull
    private Long recommendationId;

    @NotNull
    private String feedback;

    public Long getRecommendationId() { return recommendationId; }
    public void setRecommendationId(Long recommendationId) { this.recommendationId = recommendationId; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
}
