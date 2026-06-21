package com.health.diet.dto.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/* 新增饮食记录请求 */
public class DietRecordCreateCommand {

    /* 用户ID */
    private Long userId;

    /* 食物ID */
    private Long foodId;

    /* 食物名称 */
    @NotBlank
    private String foodName;

    /* 餐次类型 */
    @NotBlank
    private String mealType;

    /* 食用分量 */
    @NotNull
    private BigDecimal amount;

    /* 记录来源 */
    @NotBlank
    private String source;

    /* 食物图片URL */
    private String imageUrl;

    /* 记录时间 */
    private LocalDateTime recordTime;

    // AI 识别/分析返回的营养值（可选，后端兜底从 food_item 计算）
    /* 热量（千卡） */
    private BigDecimal calorie;
    /* 蛋白质（克） */
    private BigDecimal protein;
    /* 脂肪（克） */
    private BigDecimal fat;
    /* 碳水化合物（克） */
    private BigDecimal carbohydrate;
    /* 糖分（克） */
    private BigDecimal sugar;
    /* 钠（毫克） */
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
