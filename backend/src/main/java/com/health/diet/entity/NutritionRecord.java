package com.health.diet.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "nutrition_record", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "record_date"})
})
public class NutritionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(name = "calorie_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal calorieTotal = BigDecimal.ZERO;

    @Column(name = "protein_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal proteinTotal = BigDecimal.ZERO;

    @Column(name = "fat_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal fatTotal = BigDecimal.ZERO;

    @Column(name = "carbohydrate_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal carbohydrateTotal = BigDecimal.ZERO;

    @Column(name = "sugar_total", precision = 10, scale = 2)
    private BigDecimal sugarTotal = BigDecimal.ZERO;

    @Column(name = "sodium_total", precision = 10, scale = 2)
    private BigDecimal sodiumTotal = BigDecimal.ZERO;

    @Column(precision = 5, scale = 2)
    private BigDecimal score;

    public NutritionRecord() {}

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public LocalDate getRecordDate() { return recordDate; }
    public BigDecimal getCalorieTotal() { return calorieTotal; }
    public BigDecimal getProteinTotal() { return proteinTotal; }
    public BigDecimal getFatTotal() { return fatTotal; }
    public BigDecimal getCarbohydrateTotal() { return carbohydrateTotal; }
    public BigDecimal getSugarTotal() { return sugarTotal; }
    public BigDecimal getSodiumTotal() { return sodiumTotal; }
    public BigDecimal getScore() { return score; }

    public void setUserId(Long userId) { this.userId = userId; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }
    public void setCalorieTotal(BigDecimal calorieTotal) { this.calorieTotal = calorieTotal; }
    public void setProteinTotal(BigDecimal proteinTotal) { this.proteinTotal = proteinTotal; }
    public void setFatTotal(BigDecimal fatTotal) { this.fatTotal = fatTotal; }
    public void setCarbohydrateTotal(BigDecimal carbohydrateTotal) { this.carbohydrateTotal = carbohydrateTotal; }
    public void setSugarTotal(BigDecimal sugarTotal) { this.sugarTotal = sugarTotal; }
    public void setSodiumTotal(BigDecimal sodiumTotal) { this.sodiumTotal = sodiumTotal; }
    public void setScore(BigDecimal score) { this.score = score; }
}
