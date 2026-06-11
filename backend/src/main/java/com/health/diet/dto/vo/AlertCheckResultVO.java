package com.health.diet.dto.vo;

import java.util.List;

public class AlertCheckResultVO {

    private boolean hasAlert;
    private List<AlertMessage> alerts;

    public boolean isHasAlert() { return hasAlert; }
    public void setHasAlert(boolean hasAlert) { this.hasAlert = hasAlert; }
    public List<AlertMessage> getAlerts() { return alerts; }
    public void setAlerts(List<AlertMessage> alerts) { this.alerts = alerts; }

    public static class AlertMessage {
        private String nutrientType;
        private String message;
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
