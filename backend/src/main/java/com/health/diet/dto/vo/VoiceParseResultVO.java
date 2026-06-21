package com.health.diet.dto.vo;

import java.math.BigDecimal;
import java.util.List;

/* 语音解析结果返回 */
public class VoiceParseResultVO {

    /* 语音记录ID */
    private Long voiceRecordId;
    /* 音频URL */
    private String audioUrl;
    /* 转写文本 */
    private String transcribedText;
    /* 食物实体列表 */
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
        /* 食物名称 */
        private String foodName;
        /* 分量 */
        private double amount;
        /* 单位 */
        private String unit;
        /* 餐次类型 */
        private String mealType;
        /* 热量 */
        private BigDecimal calorie;
        /* 蛋白质 */
        private BigDecimal protein;
        /* 脂肪 */
        private BigDecimal fat;
        /* 碳水化合物 */
        private BigDecimal carbohydrate;
        /* 糖分 */
        private BigDecimal sugar;
        /* 钠 */
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
