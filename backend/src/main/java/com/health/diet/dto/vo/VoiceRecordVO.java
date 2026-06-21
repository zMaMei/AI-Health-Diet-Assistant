package com.health.diet.dto.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

/* 语音记录返回 */
public class VoiceRecordVO {

    /* 记录ID */
    private Long id;
    /* 用户ID */
    private Long userId;
    /* 记录日期 */
    private LocalDate recordDate;
    /* 音频URL */
    private String audioUrl;
    /* 转写文本 */
    private String transcribedText;
    /* 食物实体JSON */
    private String foodEntities;
    /* 时长(秒) */
    private Integer durationSeconds;
    /* 餐次类型 */
    private String mealType;
    /* 创建时间 */
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDate getRecordDate() { return recordDate; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }
    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
    public String getTranscribedText() { return transcribedText; }
    public void setTranscribedText(String transcribedText) { this.transcribedText = transcribedText; }
    public String getFoodEntities() { return foodEntities; }
    public void setFoodEntities(String foodEntities) { this.foodEntities = foodEntities; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }
    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
