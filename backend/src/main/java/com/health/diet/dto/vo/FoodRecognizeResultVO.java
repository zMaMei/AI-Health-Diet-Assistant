package com.health.diet.dto.vo;

import java.util.List;

public class FoodRecognizeResultVO {

    private List<FoodCandidate> candidates;
    private String imageUrl;

    public List<FoodCandidate> getCandidates() { return candidates; }
    public void setCandidates(List<FoodCandidate> candidates) { this.candidates = candidates; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public static class FoodCandidate {
        private String foodName;
        private double confidence;
        private String unit;
        private double defaultAmount;

        public FoodCandidate() {}
        public FoodCandidate(String foodName, double confidence, String unit, double defaultAmount) {
            this.foodName = foodName;
            this.confidence = confidence;
            this.unit = unit;
            this.defaultAmount = defaultAmount;
        }

        public String getFoodName() { return foodName; }
        public void setFoodName(String foodName) { this.foodName = foodName; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        public double getDefaultAmount() { return defaultAmount; }
        public void setDefaultAmount(double defaultAmount) { this.defaultAmount = defaultAmount; }
    }
}
