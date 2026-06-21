package com.health.diet.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/* 食物营养成分库 */
@Entity /* JPA实体 */
@Table(name = "food_item") /* 数据库表名 */
public class FoodItem {

    /* 食物主键ID */
    @Id /* 主键 */
    @GeneratedValue(strategy = GenerationType.IDENTITY) /* 自增主键 */
    private Long id;

    /* 食物名称 */
    @Column(nullable = false, length = 64, unique = true) /* 数据库列 */
    private String name;

    /* 分类 */
    @Column(nullable = false, length = 32)
    private String category;

    /* 单位 */
    @Column(nullable = false, length = 16)
    private String unit;

    /* 每100g热量 */
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal calorie = BigDecimal.ZERO;

    /* 每100g蛋白质 */
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal protein = BigDecimal.ZERO;

    /* 每100g脂肪 */
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal fat = BigDecimal.ZERO;

    /* 每100g碳水 */
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal carbohydrate = BigDecimal.ZERO;

    /* 每100g糖 */
    @Column(precision = 8, scale = 2)
    private BigDecimal sugar = BigDecimal.ZERO;

    /* 每100g钠 */
    @Column(precision = 8, scale = 2)
    private BigDecimal sodium = BigDecimal.ZERO;

    public FoodItem() {}

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getUnit() { return unit; }
    public BigDecimal getCalorie() { return calorie; }
    public BigDecimal getProtein() { return protein; }
    public BigDecimal getFat() { return fat; }
    public BigDecimal getCarbohydrate() { return carbohydrate; }
    public BigDecimal getSugar() { return sugar; }
    public BigDecimal getSodium() { return sodium; }

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setCategory(String category) { this.category = category; }
    public void setUnit(String unit) { this.unit = unit; }
    public void setCalorie(BigDecimal calorie) { this.calorie = calorie; }
    public void setProtein(BigDecimal protein) { this.protein = protein; }
    public void setFat(BigDecimal fat) { this.fat = fat; }
    public void setCarbohydrate(BigDecimal carbohydrate) { this.carbohydrate = carbohydrate; }
    public void setSugar(BigDecimal sugar) { this.sugar = sugar; }
    public void setSodium(BigDecimal sodium) { this.sodium = sodium; }
}
