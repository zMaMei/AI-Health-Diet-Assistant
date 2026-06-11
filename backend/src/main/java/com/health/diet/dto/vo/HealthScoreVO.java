package com.health.diet.dto.vo;

import java.math.BigDecimal;
import java.util.List;

public class HealthScoreVO {

    private BigDecimal score;
    private List<String> strengths;
    private List<String> risks;
    private List<String> suggestions;
    private List<ScoreHistoryPoint> history;

    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public List<String> getStrengths() { return strengths; }
    public void setStrengths(List<String> strengths) { this.strengths = strengths; }
    public List<String> getRisks() { return risks; }
    public void setRisks(List<String> risks) { this.risks = risks; }
    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
    public List<ScoreHistoryPoint> getHistory() { return history; }
    public void setHistory(List<ScoreHistoryPoint> history) { this.history = history; }

    public static class ScoreHistoryPoint {
        private String date;
        private BigDecimal score;

        public ScoreHistoryPoint() {}
        public ScoreHistoryPoint(String date, BigDecimal score) {
            this.date = date;
            this.score = score;
        }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public BigDecimal getScore() { return score; }
        public void setScore(BigDecimal score) { this.score = score; }
    }
}
