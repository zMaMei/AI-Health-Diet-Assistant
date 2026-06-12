package com.health.diet.dto.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class AlertRuleCreateCommand {

    private Long userId;

    @NotBlank
    private String nutrientType;

    @NotNull
    private BigDecimal threshold;

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
