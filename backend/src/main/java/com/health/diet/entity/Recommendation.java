package com.health.diet.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/* 推荐记录表 */
@Entity /* JPA实体 */
@Table(name = "recommendation") /* 数据库表名 */
public class Recommendation {

    /* 推荐主键ID */
    @Id /* 主键 */
    @GeneratedValue(strategy = GenerationType.IDENTITY) /* 自增主键 */
    private Long id;

    /* 关联用户ID */
    @Column(name = "user_id", nullable = false) /* 数据库列 */
    private Long userId;

    /* 菜谱ID */
    @Column(name = "recipe_id", nullable = false)
    private Long recipeId;

    /* 推荐理由 */
    @Column(nullable = false, length = 255)
    private String reason;

    /* 推荐评分 */
    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal score;

    /* 创建时间 */
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
