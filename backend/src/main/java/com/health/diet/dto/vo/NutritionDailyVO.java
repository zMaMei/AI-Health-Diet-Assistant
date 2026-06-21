package com.health.diet.dto.vo;

import java.math.BigDecimal;
import java.util.List;

/* 每日营养汇总返回 */
public class NutritionDailyVO {

    /* 热量总计 */
    private BigDecimal calorieTotal;
    /* 蛋白质总计 */
    private BigDecimal proteinTotal;
    /* 脂肪总计 */
    private BigDecimal fatTotal;
    /* 碳水化合物总计 */
    private BigDecimal carbohydrateTotal;
    /* 糖分总计 */
    private BigDecimal sugarTotal;
    /* 钠总计 */
    private BigDecimal sodiumTotal;
    /* 热量目标 */
    private BigDecimal calorieGoal;
    /* 蛋白质目标 */
    private BigDecimal proteinGoal;
    /* 脂肪目标 */
    private BigDecimal fatGoal;
    /* 碳水化合物目标 */
    private BigDecimal carbohydrateGoal;
    /* 趋势数据 */
    private List<NutritionTrendPoint> trend;
    /* 建议 */
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
        /* 日期 */
        private String date;
        /* 热量 */
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
