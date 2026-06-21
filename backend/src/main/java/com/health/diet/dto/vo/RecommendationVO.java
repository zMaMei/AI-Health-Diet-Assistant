package com.health.diet.dto.vo;

import java.math.BigDecimal;

/* 推荐菜谱返回 */
public class RecommendationVO {

    /* 推荐ID */
    private Long id;
    /* 菜谱ID */
    private Long recipeId;
    /* 菜谱名称 */
    private String recipeName;
    /* 食材 */
    private String ingredients;
    /* 步骤 */
    private String steps;
    /* 标签 */
    private String tags;
    /* 热量 */
    private BigDecimal calorie;
    /* 蛋白质 */
    private BigDecimal protein;
    /* 脂肪 */
    private BigDecimal fat;
    /* 碳水化合物 */
    private BigDecimal carbohydrate;
    /* 糖分 */
    private BigDecimal sugar;
    /* 钠 */
    private BigDecimal sodium;
    /* 推荐理由 */
    private String reason;
    /* 匹配分数 */
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
