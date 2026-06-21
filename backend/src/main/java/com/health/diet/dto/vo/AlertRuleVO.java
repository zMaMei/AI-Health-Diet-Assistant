package com.health.diet.dto.vo;

import java.math.BigDecimal;

/* 预警规则返回 */
public class AlertRuleVO {

    /* 规则ID */
    private Long id;
    /* 用户ID */
    private Long userId;
    /* 营养素类型 */
    private String nutrientType;
    /* 阈值 */
    private BigDecimal threshold;
    /* 是否启用 */
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
