package com.health.diet.dto.vo;

import java.util.List;

/* 预警检查结果返回 */
public class AlertCheckResultVO {

    /* 是否有预警 */
    private boolean hasAlert;
    /* 预警消息列表 */
    private List<AlertMessage> alerts;

    public boolean isHasAlert() { return hasAlert; }
    public void setHasAlert(boolean hasAlert) { this.hasAlert = hasAlert; }
    public List<AlertMessage> getAlerts() { return alerts; }
    public void setAlerts(List<AlertMessage> alerts) { this.alerts = alerts; }

    public static class AlertMessage {
        /* 营养素类型 */
        private String nutrientType;
        /* 预警消息 */
        private String message;
        /* 建议 */
        private String suggestion;

        public AlertMessage() {}
        public AlertMessage(String nutrientType, String message, String suggestion) {
            this.nutrientType = nutrientType;
            this.message = message;
            this.suggestion = suggestion;
        }

        public String getNutrientType() { return nutrientType; }
        public void setNutrientType(String nutrientType) { this.nutrientType = nutrientType; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getSuggestion() { return suggestion; }
        public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    }
}
