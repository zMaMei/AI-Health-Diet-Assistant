package com.health.diet.dto.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/* 创建预警规则请求 */
public class AlertRuleCreateCommand {

    /* 用户ID */
    private Long userId;

    /* 营养素类型 */
    @NotBlank
    private String nutrientType;

    /* 阈值 */
    @NotNull
    private BigDecimal threshold;

    /* 是否启用 */
    private Boolean enabled;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getNutrientType() { return nutrientType; }
    public void setNutrientType(String nutrientType) { this.nutrientType = nutrientType; }
    public BigDecimal getThreshold() { return threshold; }
    public void setThreshold(BigDecimal threshold) { this.threshold = threshold; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
