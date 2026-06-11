package com.health.diet.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "alert_rule")
public class AlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "nutrient_type", nullable = false, length = 16)
    private String nutrientType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal threshold;

    @Column(nullable = false)
    private Boolean enabled = true;

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
