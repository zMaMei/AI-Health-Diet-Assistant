package com.health.diet.dto.command;

import java.math.BigDecimal;

/* 修改饮食记录请求 */
public class DietRecordUpdateCommand {

    /* 食物ID */
    private Long foodId;
    /* 食物名称 */
    private String foodName;
    /* 餐次类型 */
    private String mealType;
    /* 食用分量 */
    private BigDecimal amount;
    /* 食物图片URL */
    private String imageUrl;

    public Long getFoodId() { return foodId; }
    public void setFoodId(Long foodId) { this.foodId = foodId; }
    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }
    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
