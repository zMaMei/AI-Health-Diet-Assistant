package com.health.diet.dto.vo;

import java.util.List;

public class VoiceParseResultVO {

    private String transcribedText;
    private List<FoodEntity> foodEntities;

    public String getTranscribedText() { return transcribedText; }
    public void setTranscribedText(String transcribedText) { this.transcribedText = transcribedText; }
    public List<FoodEntity> getFoodEntities() { return foodEntities; }
    public void setFoodEntities(List<FoodEntity> foodEntities) { this.foodEntities = foodEntities; }

    public static class FoodEntity {
        private String foodName;
        private double amount;
        private String unit;
        private String mealType;

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
    }
}
