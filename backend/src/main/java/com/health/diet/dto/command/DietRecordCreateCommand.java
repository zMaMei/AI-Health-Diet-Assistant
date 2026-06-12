package com.health.diet.dto.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DietRecordCreateCommand {

    @NotNull
    private Long userId;

    private Long foodId;

    @NotBlank
    private String foodName;

    @NotBlank
    private String mealType;

    @NotNull
    private BigDecimal amount;

    @NotBlank
    private String source;

    private String imageUrl;

    private LocalDateTime recordTime;

    // AI 识别/分析返回的营养值（可选，后端兜底从 food_item 计算）
    private BigDecimal calorie;
    private BigDecimal protein;
    private BigDecimal fat;
    private BigDecimal carbohydrate;
    private BigDecimal sugar;
    private BigDecimal sodium;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getFoodId() { return foodId; }
    public void setFoodId(Long foodId) { this.foodId = foodId; }
    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }
    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public LocalDateTime getRecordTime() { return recordTime; }
    public void setRecordTime(LocalDateTime recordTime) { this.recordTime = recordTime; }

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
