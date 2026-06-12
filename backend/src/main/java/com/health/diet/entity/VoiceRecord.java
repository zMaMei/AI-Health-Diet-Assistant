package com.health.diet.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "voice_record")
public class VoiceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(name = "audio_url", nullable = false, length = 255)
    private String audioUrl;

    @Column(name = "transcribed_text", columnDefinition = "TEXT")
    private String transcribedText;

    @Column(name = "food_entities", columnDefinition = "TEXT")
    private String foodEntities;

    @Column(name = "duration_seconds")
    private Integer durationSeconds = 0;

    @Column(name = "meal_type", length = 16)
    private String mealType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public VoiceRecord() {}

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public LocalDate getRecordDate() { return recordDate; }
    public String getAudioUrl() { return audioUrl; }
    public String getTranscribedText() { return transcribedText; }
    public String getFoodEntities() { return foodEntities; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public String getMealType() { return mealType; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
    public void setTranscribedText(String transcribedText) { this.transcribedText = transcribedText; }
    public void setFoodEntities(String foodEntities) { this.foodEntities = foodEntities; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }
    public void setMealType(String mealType) { this.mealType = mealType; }
}
