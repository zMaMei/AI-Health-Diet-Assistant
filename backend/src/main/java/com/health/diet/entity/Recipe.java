package com.health.diet.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/* 食谱库表 */
@Entity /* JPA实体 */
@Table(name = "recipe") /* 数据库表名 */
public class Recipe {

    /* 食谱主键ID */
    @Id /* 主键 */
    @GeneratedValue(strategy = GenerationType.IDENTITY) /* 自增主键 */
    private Long id;

    /* 菜谱名称 */
    @Column(nullable = false, length = 64, unique = true) /* 数据库列 */
    private String name;

    /* 食材列表 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String ingredients;

    /* 做法步骤 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String steps;

    /* 标签 */
    @Column(length = 255)
    private String tags;

    /* 热量 */
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal calorie = BigDecimal.ZERO;

    /* 蛋白质 */
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal protein = BigDecimal.ZERO;

    /* 脂肪 */
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal fat = BigDecimal.ZERO;

    /* 碳水 */
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal carbohydrate = BigDecimal.ZERO;

    /* 糖 */
    @Column(precision = 8, scale = 2)
    private BigDecimal sugar = BigDecimal.ZERO;

    /* 钠 */
    @Column(precision = 8, scale = 2)
    private BigDecimal sodium = BigDecimal.ZERO;

    public Recipe() {}

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getIngredients() { return ingredients; }
    public String getSteps() { return steps; }
    public String getTags() { return tags; }
    public BigDecimal getCalorie() { return calorie; }
    public BigDecimal getProtein() { return protein; }
    public BigDecimal getFat() { return fat; }
    public BigDecimal getCarbohydrate() { return carbohydrate; }
    public BigDecimal getSugar() { return sugar; }
    public BigDecimal getSodium() { return sodium; }

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setIngredients(String ingredients) { this.ingredients = ingredients; }
    public void setSteps(String steps) { this.steps = steps; }
    public void setTags(String tags) { this.tags = tags; }
    public void setCalorie(BigDecimal calorie) { this.calorie = calorie; }
    public void setProtein(BigDecimal protein) { this.protein = protein; }
    public void setFat(BigDecimal fat) { this.fat = fat; }
    public void setCarbohydrate(BigDecimal carbohydrate) { this.carbohydrate = carbohydrate; }
    public void setSugar(BigDecimal sugar) { this.sugar = sugar; }
    public void setSodium(BigDecimal sodium) { this.sodium = sodium; }
}
