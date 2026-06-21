package com.health.diet.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/* 语音记录表 */
@Entity /* JPA实体 */
@Table(name = "voice_record") /* 数据库表名 */
public class VoiceRecord {

    /* 语音主键ID */
    @Id /* 主键 */
    @GeneratedValue(strategy = GenerationType.IDENTITY) /* 自增主键 */
    private Long id;

    /* 关联用户ID */
    @Column(name = "user_id", nullable = false) /* 数据库列 */
    private Long userId;

    /* 记录日期 */
    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    /* 音频文件路径 */
    @Column(name = "audio_url", nullable = false, length = 255)
    private String audioUrl;

    /* 转写文本 */
    @Column(name = "transcribed_text", columnDefinition = "TEXT")
    private String transcribedText;

    /* 食物实体JSON */
    @Column(name = "food_entities", columnDefinition = "TEXT")
    private String foodEntities;

    /* 录音时长 */
    @Column(name = "duration_seconds")
    private Integer durationSeconds = 0;

    /* 餐次类型 */
    @Column(name = "meal_type", length = 16)
    private String mealType;

    /* 创建时间 */
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
