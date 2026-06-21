package com.health.diet.dto.vo;

import java.math.BigDecimal;
import java.util.List;

/* 健康评分返回 */
public class HealthScoreVO {

    /* 评分 */
    private BigDecimal score;
    /* 优势项 */
    private List<String> strengths;
    /* 风险项 */
    private List<String> risks;
    /* 建议 */
    private List<String> suggestions;
    /* 历史评分 */
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
        /* 日期 */
        private String date;
        /* 评分 */
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
