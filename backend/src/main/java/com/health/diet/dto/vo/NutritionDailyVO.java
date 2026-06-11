package com.health.diet.dto.vo;

import java.math.BigDecimal;
import java.util.List;

public class NutritionDailyVO {

    private BigDecimal calorieTotal;
    private BigDecimal proteinTotal;
    private BigDecimal fatTotal;
    private BigDecimal carbohydrateTotal;
    private BigDecimal sugarTotal;
    private BigDecimal sodiumTotal;
    private BigDecimal calorieGoal;
    private BigDecimal proteinGoal;
    private BigDecimal fatGoal;
    private BigDecimal carbohydrateGoal;
    private List<NutritionTrendPoint> trend;
    private String suggestion;

    public BigDecimal getCalorieTotal() { return calorieTotal; }
    public void setCalorieTotal(BigDecimal calorieTotal) { this.calorieTotal = calorieTotal; }
    public BigDecimal getProteinTotal() { return proteinTotal; }
    public void setProteinTotal(BigDecimal proteinTotal) { this.proteinTotal = proteinTotal; }
    public BigDecimal getFatTotal() { return fatTotal; }
    public void setFatTotal(BigDecimal fatTotal) { this.fatTotal = fatTotal; }
    public BigDecimal getCarbohydrateTotal() { return carbohydrateTotal; }
    public void setCarbohydrateTotal(BigDecimal carbohydrateTotal) { this.carbohydrateTotal = carbohydrateTotal; }
    public BigDecimal getSugarTotal() { return sugarTotal; }
    public void setSugarTotal(BigDecimal sugarTotal) { this.sugarTotal = sugarTotal; }
    public BigDecimal getSodiumTotal() { return sodiumTotal; }
    public void setSodiumTotal(BigDecimal sodiumTotal) { this.sodiumTotal = sodiumTotal; }
    public BigDecimal getCalorieGoal() { return calorieGoal; }
    public void setCalorieGoal(BigDecimal calorieGoal) { this.calorieGoal = calorieGoal; }
    public BigDecimal getProteinGoal() { return proteinGoal; }
    public void setProteinGoal(BigDecimal proteinGoal) { this.proteinGoal = proteinGoal; }
    public BigDecimal getFatGoal() { return fatGoal; }
    public void setFatGoal(BigDecimal fatGoal) { this.fatGoal = fatGoal; }
    public BigDecimal getCarbohydrateGoal() { return carbohydrateGoal; }
    public void setCarbohydrateGoal(BigDecimal carbohydrateGoal) { this.carbohydrateGoal = carbohydrateGoal; }
    public List<NutritionTrendPoint> getTrend() { return trend; }
    public void setTrend(List<NutritionTrendPoint> trend) { this.trend = trend; }
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }

    public static class NutritionTrendPoint {
        private String date;
        private BigDecimal calorie;

        public NutritionTrendPoint() {}
        public NutritionTrendPoint(String date, BigDecimal calorie) {
            this.date = date;
            this.calorie = calorie;
        }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public BigDecimal getCalorie() { return calorie; }
        public void setCalorie(BigDecimal calorie) { this.calorie = calorie; }
    }
}
