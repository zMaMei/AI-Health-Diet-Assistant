package com.health.diet.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/* 饮食记录表 */
@Entity /* JPA实体 */
@Table(name = "diet_record") /* 数据库表名 */
public class DietRecord {

    /* 记录主键ID */
    @Id /* 主键 */
    @GeneratedValue(strategy = GenerationType.IDENTITY) /* 自增主键 */
    private Long id;

    /* 关联用户ID */
    @Column(name = "user_id", nullable = false) /* 数据库列 */
    private Long userId;

    /* 食物ID */
    @Column(name = "food_id")
    private Long foodId;

    /* 食物名称 */
    @Column(name = "food_name", nullable = false, length = 64)
    private String foodName;

    /* 餐次类型 */
    @Column(name = "meal_type", nullable = false, length = 16)
    private String mealType;

    /* 份量 */
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal amount;

    /* 来源（拍照/语音/手动） */
    @Column(nullable = false, length = 16)
    private String source;

    /* 图片URL */
    @Column(name = "image_url", length = 255)
    private String imageUrl;

    /* 记录时间 */
    @Column(name = "record_time", nullable = false)
    private LocalDateTime recordTime;

    /* 创建时间 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /* 热量快照（每100g/单位的实际摄入量 = food_item营养 × amount/100） */
    @Column(precision = 8, scale = 2)
    private BigDecimal calorie = BigDecimal.ZERO;

    /* 蛋白质快照 */
    @Column(precision = 8, scale = 2)
    private BigDecimal protein = BigDecimal.ZERO;

    /* 脂肪快照 */
    @Column(precision = 8, scale = 2)
    private BigDecimal fat = BigDecimal.ZERO;

    /* 碳水快照 */
    @Column(precision = 8, scale = 2)
    private BigDecimal carbohydrate = BigDecimal.ZERO;

    /* 糖快照 */
    @Column(precision = 8, scale = 2)
    private BigDecimal sugar = BigDecimal.ZERO;

    /* 钠快照 */
    @Column(precision = 8, scale = 2)
    private BigDecimal sodium = BigDecimal.ZERO;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public DietRecord() {}

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getFoodId() { return foodId; }
    public String getFoodName() { return foodName; }
    public String getMealType() { return mealType; }
    public BigDecimal getAmount() { return amount; }
    public String getSource() { return source; }
    public String getImageUrl() { return imageUrl; }
    public LocalDateTime getRecordTime() { return recordTime; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public BigDecimal getCalorie() { return calorie; }
    public BigDecimal getProtein() { return protein; }
    public BigDecimal getFat() { return fat; }
    public BigDecimal getCarbohydrate() { return carbohydrate; }
    public BigDecimal getSugar() { return sugar; }
    public BigDecimal getSodium() { return sodium; }

    public void setId(Long id) { this.id = id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setFoodId(Long foodId) { this.foodId = foodId; }
    public void setFoodName(String foodName) { this.foodName = foodName; }
    public void setMealType(String mealType) { this.mealType = mealType; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setSource(String source) { this.source = source; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setRecordTime(LocalDateTime recordTime) { this.recordTime = recordTime; }
    public void setCalorie(BigDecimal calorie) { this.calorie = calorie; }
    public void setProtein(BigDecimal protein) { this.protein = protein; }
    public void setFat(BigDecimal fat) { this.fat = fat; }
    public void setCarbohydrate(BigDecimal carbohydrate) { this.carbohydrate = carbohydrate; }
    public void setSugar(BigDecimal sugar) { this.sugar = sugar; }
    public void setSodium(BigDecimal sodium) { this.sodium = sodium; }
}
