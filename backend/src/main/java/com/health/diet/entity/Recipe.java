package com.health.diet.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "recipe")
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64, unique = true)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String ingredients;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String steps;

    @Column(length = 255)
    private String tags;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal calorie = BigDecimal.ZERO;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal protein = BigDecimal.ZERO;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal fat = BigDecimal.ZERO;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal carbohydrate = BigDecimal.ZERO;

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

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setIngredients(String ingredients) { this.ingredients = ingredients; }
    public void setSteps(String steps) { this.steps = steps; }
    public void setTags(String tags) { this.tags = tags; }
    public void setCalorie(BigDecimal calorie) { this.calorie = calorie; }
    public void setProtein(BigDecimal protein) { this.protein = protein; }
    public void setFat(BigDecimal fat) { this.fat = fat; }
    public void setCarbohydrate(BigDecimal carbohydrate) { this.carbohydrate = carbohydrate; }
}
