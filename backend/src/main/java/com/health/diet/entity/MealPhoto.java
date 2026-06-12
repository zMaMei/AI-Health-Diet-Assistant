package com.health.diet.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "meal_photo")
public class MealPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(name = "meal_type", nullable = false, length = 16)
    private String mealType;

    @Column(name = "image_url", nullable = false, length = 255)
    private String imageUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public MealPhoto() {}

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public LocalDate getRecordDate() { return recordDate; }
    public String getMealType() { return mealType; }
    public String getImageUrl() { return imageUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }
    public void setMealType(String mealType) { this.mealType = mealType; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
