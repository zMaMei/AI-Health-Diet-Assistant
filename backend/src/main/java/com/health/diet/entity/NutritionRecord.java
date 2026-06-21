package com.health.diet.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/* 每日营养汇总表 */
@Entity /* JPA实体 */
@Table(name = "nutrition_record", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "record_date"})
}) /* 数据库表名 */
public class NutritionRecord {

    /* 汇总主键ID */
    @Id /* 主键 */
    @GeneratedValue(strategy = GenerationType.IDENTITY) /* 自增主键 */
    private Long id;

    /* 关联用户ID */
    @Column(name = "user_id", nullable = false) /* 数据库列 */
    private Long userId;

    /* 记录日期 */
    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    /* 总热量 */
    @Column(name = "calorie_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal calorieTotal = BigDecimal.ZERO;

    /* 总蛋白质 */
    @Column(name = "protein_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal proteinTotal = BigDecimal.ZERO;

    /* 总脂肪 */
    @Column(name = "fat_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal fatTotal = BigDecimal.ZERO;

    /* 总碳水 */
    @Column(name = "carbohydrate_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal carbohydrateTotal = BigDecimal.ZERO;

    /* 总糖 */
    @Column(name = "sugar_total", precision = 10, scale = 2)
    private BigDecimal sugarTotal = BigDecimal.ZERO;

    /* 总钠 */
    @Column(name = "sodium_total", precision = 10, scale = 2)
    private BigDecimal sodiumTotal = BigDecimal.ZERO;

    /* 评分 */
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
