package com.health.diet.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "diet_record")
public class DietRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "food_id")
    private Long foodId;

    @Column(name = "food_name", nullable = false, length = 64)
    private String foodName;

    @Column(name = "meal_type", nullable = false, length = 16)
    private String mealType;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 16)
    private String source;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(name = "record_time", nullable = false)
    private LocalDateTime recordTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

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

    public void setId(Long id) { this.id = id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setFoodId(Long foodId) { this.foodId = foodId; }
    public void setFoodName(String foodName) { this.foodName = foodName; }
    public void setMealType(String mealType) { this.mealType = mealType; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setSource(String source) { this.source = source; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setRecordTime(LocalDateTime recordTime) { this.recordTime = recordTime; }
}
