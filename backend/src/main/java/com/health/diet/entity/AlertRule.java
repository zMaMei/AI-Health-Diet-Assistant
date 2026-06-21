package com.health.diet.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/* 预警规则表 */
@Entity /* JPA实体 */
@Table(name = "alert_rule") /* 数据库表名 */
public class AlertRule {

    /* 规则主键ID */
    @Id /* 主键 */
    @GeneratedValue(strategy = GenerationType.IDENTITY) /* 自增主键 */
    private Long id;

    /* 关联用户ID */
    @Column(name = "user_id", nullable = false) /* 数据库列 */
    private Long userId;

    /* 营养素类型 */
    @Column(name = "nutrient_type", nullable = false, length = 16)
    private String nutrientType;

    /* 阈值 */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal threshold;

    /* 是否启用 */
    @Column(nullable = false)
    private Boolean enabled = true;

    /* 更新时间 */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public AlertRule() {}

    public void enable() { this.enabled = true; }
    public void disable() { this.enabled = false; }
    public void updateThreshold(BigDecimal threshold) { this.threshold = threshold; }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getNutrientType() { return nutrientType; }
    public BigDecimal getThreshold() { return threshold; }
    public Boolean getEnabled() { return enabled; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setUserId(Long userId) { this.userId = userId; }
    public void setNutrientType(String nutrientType) { this.nutrientType = nutrientType; }
    public void setThreshold(BigDecimal threshold) { this.threshold = threshold; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
