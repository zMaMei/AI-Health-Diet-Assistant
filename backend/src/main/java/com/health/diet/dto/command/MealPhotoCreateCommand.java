package com.health.diet.dto.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/* 新增餐次照片请求 */
public class MealPhotoCreateCommand {

    /* 用户ID */
    private Long userId;

    /* 记录日期 */
    @NotNull
    private LocalDate recordDate;

    /* 餐次类型 */
    @NotBlank
    private String mealType;

    /* 图片URL */
    @NotBlank
    private String imageUrl;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDate getRecordDate() { return recordDate; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }
    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
