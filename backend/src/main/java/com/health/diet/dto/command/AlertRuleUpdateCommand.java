package com.health.diet.dto.command;

import java.math.BigDecimal;

/* 修改预警规则请求 */
public class AlertRuleUpdateCommand {

    /* 阈值 */
    private BigDecimal threshold;
    /* 是否启用 */
    private Boolean enabled;

    public BigDecimal getThreshold() { return threshold; }
    public void setThreshold(BigDecimal threshold) { this.threshold = threshold; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
