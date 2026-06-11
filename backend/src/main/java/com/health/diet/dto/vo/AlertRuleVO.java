package com.health.diet.dto.vo;

import java.math.BigDecimal;

public class AlertRuleVO {

    private Long id;
    private Long userId;
    private String nutrientType;
    private BigDecimal threshold;
    private Boolean enabled;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getNutrientType() { return nutrientType; }
    public void setNutrientType(String nutrientType) { this.nutrientType = nutrientType; }
    public BigDecimal getThreshold() { return threshold; }
    public void setThreshold(BigDecimal threshold) { this.threshold = threshold; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
