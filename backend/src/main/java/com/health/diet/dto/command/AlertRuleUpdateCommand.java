package com.health.diet.dto.command;

import java.math.BigDecimal;

public class AlertRuleUpdateCommand {

    private BigDecimal threshold;
    private Boolean enabled;

    public BigDecimal getThreshold() { return threshold; }
    public void setThreshold(BigDecimal threshold) { this.threshold = threshold; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
