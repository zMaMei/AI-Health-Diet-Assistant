package com.health.diet.dto.vo;

import java.math.BigDecimal;

public class RecommendationVO {

    private Long id;
    private Long recipeId;
    private String recipeName;
    private String ingredients;
    private String steps;
    private String tags;
    private BigDecimal calorie;
    private BigDecimal protein;
    private BigDecimal fat;
    private BigDecimal carbohydrate;
    private BigDecimal sugar;
    private BigDecimal sodium;
    private String reason;
    private BigDecimal matchScore;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRecipeId() { return recipeId; }
    public void setRecipeId(Long recipeId) { this.recipeId = recipeId; }
    public String getRecipeName() { return recipeName; }
    public void setRecipeName(String recipeName) { this.recipeName = recipeName; }
    public String getIngredients() { return ingredients; }
    public void setIngredients(String ingredients) { this.ingredients = ingredients; }
    public String getSteps() { return steps; }
    public void setSteps(String steps) { this.steps = steps; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public BigDecimal getCalorie() { return calorie; }
    public void setCalorie(BigDecimal calorie) { this.calorie = calorie; }
    public BigDecimal getProtein() { return protein; }
    public void setProtein(BigDecimal protein) { this.protein = protein; }
    public BigDecimal getFat() { return fat; }
    public void setFat(BigDecimal fat) { this.fat = fat; }
    public BigDecimal getCarbohydrate() { return carbohydrate; }
    public void setCarbohydrate(BigDecimal carbohydrate) { this.carbohydrate = carbohydrate; }
    public BigDecimal getSugar() { return sugar; }
    public void setSugar(BigDecimal sugar) { this.sugar = sugar; }
    public BigDecimal getSodium() { return sodium; }
    public void setSodium(BigDecimal sodium) { this.sodium = sodium; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public BigDecimal getMatchScore() { return matchScore; }
    public void setMatchScore(BigDecimal matchScore) { this.matchScore = matchScore; }
}
