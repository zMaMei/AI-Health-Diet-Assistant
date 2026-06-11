package com.health.diet.dto.vo;

import java.math.BigDecimal;
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
        private NutritionPreview nutritionPreview;
        private String category;

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
        public NutritionPreview getNutritionPreview() { return nutritionPreview; }
        public void setNutritionPreview(NutritionPreview nutritionPreview) { this.nutritionPreview = nutritionPreview; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
    }

    /**
     * Per-serving nutrition estimate from AI analysis.
     * Values reflect the nutrition for the default serving size.
     */
    public static class NutritionPreview {
        private BigDecimal calorie;
        private BigDecimal protein;
        private BigDecimal fat;
        private BigDecimal carbohydrate;
        private BigDecimal sugar;
        private BigDecimal sodium;

        public NutritionPreview() {}
        public NutritionPreview(BigDecimal calorie, BigDecimal protein, BigDecimal fat,
                                BigDecimal carbohydrate, BigDecimal sugar, BigDecimal sodium) {
            this.calorie = calorie;
            this.protein = protein;
            this.fat = fat;
            this.carbohydrate = carbohydrate;
            this.sugar = sugar;
            this.sodium = sodium;
        }

        public BigDecimal getCalorie() { return calorie; }
        public void setCalorie(BigDecimal calorie) { this.calorie = calorie; }
        public BigDecimal getProtein() { return protein; }
        public void setProtein(BigDecimal protein) { this.protein = protein; }
        public BigDecimal getFat() { return fat; }
        public void setFat(BigDecimal fat) { this.fat = fat; }
        public BigDecimal getCarbohydrate() { return carbohydrate; }
        public void setCarbohydrate(BigDecimal carbohydrate) { this.carbohydrate = carbohydrate; }
        public BigDecimal getSugar() { return sugar; }
        public void setSugar(BigDecimal sugar) { this.sugar = sugar; }
        public BigDecimal getSodium() { return sodium; }
        public void setSodium(BigDecimal sodium) { this.sodium = sodium; }
    }
}
