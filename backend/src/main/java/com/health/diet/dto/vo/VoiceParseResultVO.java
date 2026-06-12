package com.health.diet.dto.vo;

import java.math.BigDecimal;
import java.util.List;

public class VoiceParseResultVO {

    private Long voiceRecordId;
    private String audioUrl;
    private String transcribedText;
    private List<FoodEntity> foodEntities;

    public Long getVoiceRecordId() { return voiceRecordId; }
    public void setVoiceRecordId(Long voiceRecordId) { this.voiceRecordId = voiceRecordId; }
    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
    public String getTranscribedText() { return transcribedText; }
    public void setTranscribedText(String transcribedText) { this.transcribedText = transcribedText; }
    public List<FoodEntity> getFoodEntities() { return foodEntities; }
    public void setFoodEntities(List<FoodEntity> foodEntities) { this.foodEntities = foodEntities; }

    public static class FoodEntity {
        private String foodName;
        private double amount;
        private String unit;
        private String mealType;
        private BigDecimal calorie;
        private BigDecimal protein;
        private BigDecimal fat;
        private BigDecimal carbohydrate;
        private BigDecimal sugar;
        private BigDecimal sodium;

        public FoodEntity() {}
        public FoodEntity(String foodName, double amount, String unit, String mealType) {
            this.foodName = foodName;
            this.amount = amount;
            this.unit = unit;
            this.mealType = mealType;
        }

        public String getFoodName() { return foodName; }
        public void setFoodName(String foodName) { this.foodName = foodName; }
        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        public String getMealType() { return mealType; }
        public void setMealType(String mealType) { this.mealType = mealType; }
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
