package com.health.diet.dto.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

/* 餐次照片返回 */
public class MealPhotoVO {

    /* 照片ID */
    private Long id;
    /* 用户ID */
    private Long userId;
    /* 记录日期 */
    private LocalDate recordDate;
    /* 餐次类型 */
    private String mealType;
    /* 图片URL */
    private String imageUrl;
    /* 创建时间 */
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDate getRecordDate() { return recordDate; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }
    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
