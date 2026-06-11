package com.health.diet.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "food_item")
public class FoodItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64, unique = true)
    private String name;

    @Column(nullable = false, length = 32)
    private String category;

    @Column(nullable = false, length = 16)
    private String unit;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal calorie = BigDecimal.ZERO;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal protein = BigDecimal.ZERO;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal fat = BigDecimal.ZERO;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal carbohydrate = BigDecimal.ZERO;

    @Column(precision = 8, scale = 2)
    private BigDecimal sugar = BigDecimal.ZERO;

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
