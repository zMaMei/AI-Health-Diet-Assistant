package com.health.diet.dto.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/* 饮食记录返回 */
public class DietRecordVO {

    /* 记录ID */
    private Long id;
    /* 用户ID */
    private Long userId;
    /* 食物ID */
    private Long foodId;
    /* 食物名称 */
    private String foodName;
    /* 餐次类型 */
    private String mealType;
    /* 分量 */
    private BigDecimal amount;
    /* 记录来源 */
    private String source;
    /* 图片URL */
    private String imageUrl;
    /* 记录时间 */
    private LocalDateTime recordTime;
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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
