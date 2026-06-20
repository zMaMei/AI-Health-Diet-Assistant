package com.health.diet.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "recommendation")
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "recipe_id", nullable = false)
    private Long recipeId;

    @Column(nullable = false, length = 255)
    private String reason;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal score;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Recommendation() {}

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getRecipeId() { return recipeId; }
    public String getReason() { return reason; }
    public BigDecimal getScore() { return score; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setUserId(Long userId) { this.userId = userId; }
    public void setRecipeId(Long recipeId) { this.recipeId = recipeId; }
    public void setReason(String reason) { this.reason = reason; }
    public void setScore(BigDecimal score) { this.score = score; }
}
